package com.flashsale.service;

import com.flashsale.dto.ReservationRequest;
import com.flashsale.dto.ReservationResponse;
import com.flashsale.model.Reservation;
import com.flashsale.model.TicketTier;
import com.flashsale.model.enums.ConcurrencyStrategy;
import com.flashsale.model.enums.ReservationStatus;
import com.flashsale.repository.ReservationRepository;
import com.flashsale.repository.TicketTierRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReservationService {

    private final TicketTierRepository ticketTierRepository;
    private final ReservationRepository reservationRepository;
    private final RateLimiterService rateLimiterService;
    private final ConcurrencyLockService concurrencyLockService;
    private final IdempotencyService idempotencyService;

    @Value("${flashsale.reservation.timeout-minutes:10}")
    private int timeoutMinutes;

    @Value("${flashsale.concurrency.default-strategy:ATOMIC_DB_CONDITIONAL}")
    private ConcurrencyStrategy defaultStrategy;

    public ReservationService(TicketTierRepository ticketTierRepository,
                              ReservationRepository reservationRepository,
                              RateLimiterService rateLimiterService,
                              ConcurrencyLockService concurrencyLockService,
                              IdempotencyService idempotencyService) {
        this.ticketTierRepository = ticketTierRepository;
        this.reservationRepository = reservationRepository;
        this.rateLimiterService = rateLimiterService;
        this.concurrencyLockService = concurrencyLockService;
        this.idempotencyService = idempotencyService;
    }

    @Transactional
    public ReservationResponse reserveTickets(ReservationRequest request, String clientIp) {
        // 1. Rate Limiting Check
        if (!rateLimiterService.tryAcquire(clientIp != null ? clientIp : request.userEmail())) {
            throw new RuntimeException("Rate limit exceeded. Please wait a few moments before retrying.");
        }

        // 2. Idempotency Check (Prevent duplicate charges on double-click)
        Optional<Reservation> existingReservation = reservationRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existingReservation.isPresent()) {
            return mapToResponse(existingReservation.get());
        }

        ConcurrencyStrategy strategy = request.strategy() != null ? request.strategy() : defaultStrategy;
        TicketTier tier = ticketTierRepository.findById(request.ticketTierId())
                .orElseThrow(() -> new IllegalArgumentException("Ticket tier not found for ID: " + request.ticketTierId()));

        // 3. Concurrency Strategy Execution
        boolean reserved = switch (strategy) {
            case ATOMIC_DB_CONDITIONAL -> executeAtomicDbDecrement(request.ticketTierId(), request.quantity());
            case PESSIMISTIC_LOCK -> executePessimisticLockDecrement(request.ticketTierId(), request.quantity());
            case STRIPED_LOCK -> executeStripedLockDecrement(request.ticketTierId(), request.quantity());
            case OPTIMISTIC_LOCK -> executeOptimisticLockDecrement(request.ticketTierId(), request.quantity());
        };

        if (!reserved) {
            throw new IllegalStateException("Tickets are sold out or insufficient seats available in this tier.");
        }

        // 4. Create and persist reservation hold
        String token = "RES-" + UUID.randomUUID().toString().substring(0, 18).toUpperCase();
        BigDecimal totalAmount = tier.getPrice().multiply(BigDecimal.valueOf(request.quantity()));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(timeoutMinutes);

        Reservation reservation = new Reservation(
                token,
                tier,
                request.userEmail(),
                request.quantity(),
                totalAmount,
                request.idempotencyKey(),
                expiresAt
        );

        Reservation saved = reservationRepository.save(reservation);
        idempotencyService.registerKey(request.idempotencyKey(), saved.getId());

        return mapToResponse(saved);
    }

    private boolean executeAtomicDbDecrement(Long tierId, int quantity) {
        int rowsUpdated = ticketTierRepository.decrementAvailableSeatsAtomic(tierId, quantity);
        return rowsUpdated > 0;
    }

    private boolean executePessimisticLockDecrement(Long tierId, int quantity) {
        Optional<TicketTier> tierOpt = ticketTierRepository.findByIdWithPessimisticLock(tierId);
        if (tierOpt.isEmpty() || tierOpt.get().getAvailableSeats() < quantity) {
            return false;
        }
        TicketTier tier = tierOpt.get();
        tier.setAvailableSeats(tier.getAvailableSeats() - quantity);
        ticketTierRepository.save(tier);
        return true;
    }

    private boolean executeStripedLockDecrement(Long tierId, int quantity) {
        boolean lockAcquired = concurrencyLockService.tryLock(tierId, 1500);
        if (!lockAcquired) {
            return false;
        }
        try {
            int rowsUpdated = ticketTierRepository.decrementAvailableSeatsAtomic(tierId, quantity);
            return rowsUpdated > 0;
        } finally {
            concurrencyLockService.unlock(tierId);
        }
    }

    private boolean executeOptimisticLockDecrement(Long tierId, int quantity) {
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                TicketTier tier = ticketTierRepository.findById(tierId)
                        .orElseThrow(() -> new IllegalArgumentException("Tier not found"));
                if (tier.getAvailableSeats() < quantity) {
                    return false;
                }
                tier.setAvailableSeats(tier.getAvailableSeats() - quantity);
                ticketTierRepository.save(tier);
                return true;
            } catch (Exception e) {
                // Version mismatch collision, retry
                try {
                    Thread.sleep(10 + (long)(Math.random() * 20));
                } catch (InterruptedException ignored) {}
            }
        }
        return false;
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservation(String token) {
        Reservation reservation = reservationRepository.findByReservationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found for token: " + token));
        return mapToResponse(reservation);
    }

    @Transactional
    public void cancelReservation(String token) {
        Reservation reservation = reservationRepository.findByReservationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        if (reservation.getStatus() == ReservationStatus.PENDING) {
            reservation.setStatus(ReservationStatus.CANCELLED);
            ticketTierRepository.incrementAvailableSeatsAtomic(reservation.getTicketTier().getId(), reservation.getQuantity());
            reservationRepository.save(reservation);
            idempotencyService.removeKey(reservation.getIdempotencyKey());
        }
    }

    @Transactional
    public int releaseExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<Reservation> expiredList = reservationRepository.findByStatusAndExpiresAtBefore(ReservationStatus.PENDING, now);
        int count = 0;
        for (Reservation res : expiredList) {
            res.setStatus(ReservationStatus.EXPIRED);
            ticketTierRepository.incrementAvailableSeatsAtomic(res.getTicketTier().getId(), res.getQuantity());
            reservationRepository.save(res);
            idempotencyService.removeKey(res.getIdempotencyKey());
            count++;
        }
        return count;
    }

    public ReservationResponse mapToResponse(Reservation r) {
        long remainingSec = 0;
        if (r.getStatus() == ReservationStatus.PENDING) {
            Duration dur = Duration.between(LocalDateTime.now(), r.getExpiresAt());
            remainingSec = Math.max(0, dur.getSeconds());
        }

        return new ReservationResponse(
                r.getReservationToken(),
                r.getTicketTier().getEvent().getId(),
                r.getTicketTier().getEvent().getTitle(),
                r.getTicketTier().getId(),
                r.getTicketTier().getName(),
                r.getQuantity(),
                r.getTicketTier().getPrice(),
                r.getTotalAmount(),
                r.getStatus(),
                r.getCreatedAt(),
                r.getExpiresAt(),
                remainingSec
        );
    }
}
