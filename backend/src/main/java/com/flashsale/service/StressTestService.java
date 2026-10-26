package com.flashsale.service;

import com.flashsale.dto.ReservationRequest;
import com.flashsale.dto.StressTestRequest;
import com.flashsale.dto.StressTestResult;
import com.flashsale.model.TicketTier;
import com.flashsale.model.enums.ConcurrencyStrategy;
import com.flashsale.repository.TicketTierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Concurrency Benchmark & Load Simulator
 * Fires N concurrent buyer threads at the exact same millisecond using a CountDownLatch.
 * Demonstrates mathematically that 0 items are oversold under massive contention.
 */
@Service
public class StressTestService {

    private final TicketTierRepository ticketTierRepository;
    private final ReservationService reservationService;

    public StressTestService(TicketTierRepository ticketTierRepository, ReservationService reservationService) {
        this.ticketTierRepository = ticketTierRepository;
        this.reservationService = reservationService;
    }

    public StressTestResult runBenchmark(StressTestRequest request) {
        TicketTier tier = ticketTierRepository.findById(request.ticketTierId())
                .orElseThrow(() -> new IllegalArgumentException("Tier not found"));

        int initialAvailable = tier.getAvailableSeats();
        int totalUsers = request.totalConcurrentUsers();
        int qtyPerUser = request.quantityPerUser();
        ConcurrencyStrategy strategy = request.strategy() != null ? request.strategy() : ConcurrencyStrategy.ATOMIC_DB_CONDITIONAL;

        AtomicInteger successCounter = new AtomicInteger(0);
        AtomicInteger rejectedCounter = new AtomicInteger(0);

        int poolSize = Math.min(100, totalUsers);
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        CountDownLatch readyLatch = new CountDownLatch(totalUsers);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalUsers);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < totalUsers; i++) {
            final int index = i;
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    // All threads wait here until gate opens -> fires simultaneously!
                    startGate.await();

                    String idempotencyKey = "STRESS-" + UUID.randomUUID();
                    String userEmail = "buyer_" + index + "@benchmark.test";

                    ReservationRequest resReq = new ReservationRequest(
                            tier.getId(),
                            qtyPerUser,
                            userEmail,
                            idempotencyKey,
                            strategy
                    );

                    reservationService.reserveTickets(resReq, "127.0.0.1");
                    successCounter.incrementAndGet();
                } catch (Exception e) {
                    rejectedCounter.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        try {
            readyLatch.await(5, TimeUnit.SECONDS);
            startGate.countDown(); // FIRE ALL THREADS!
            doneLatch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdownNow();
        }

        long durationMs = Math.max(1, System.currentTimeMillis() - startTime);

        // Fetch refreshed tier state from database
        TicketTier refreshedTier = ticketTierRepository.findById(request.ticketTierId()).orElse(tier);
        int finalAvailable = refreshedTier.getAvailableSeats();

        int totalSeatsSold = successCounter.get() * qtyPerUser;
        int expectedRemaining = Math.max(0, initialAvailable - totalSeatsSold);
        int oversoldCount = (finalAvailable < 0) ? Math.abs(finalAvailable) : Math.max(0, totalSeatsSold - initialAvailable);

        double rps = (totalUsers / (double) durationMs) * 1000.0;

        String statusMsg = (oversoldCount == 0)
                ? "TEST PASSED: 0 overselling verified across " + totalUsers + " concurrent threads!"
                : "FAILED: Overselling detected!";

        return new StressTestResult(
                tier.getId(),
                tier.getName(),
                initialAvailable,
                finalAvailable,
                totalUsers,
                successCounter.get(),
                rejectedCounter.get(),
                totalSeatsSold,
                oversoldCount,
                durationMs,
                Math.round(rps * 100.0) / 100.0,
                strategy,
                statusMsg
        );
    }

    @Transactional
    public void resetInventory(Long tierId, Integer seats) {
        TicketTier tier = ticketTierRepository.findById(tierId)
                .orElseThrow(() -> new IllegalArgumentException("Tier not found"));
        int newTotal = seats != null ? seats : tier.getTotalSeats();
        tier.setTotalSeats(newTotal);
        tier.setAvailableSeats(newTotal);
        ticketTierRepository.save(tier);
    }
}
