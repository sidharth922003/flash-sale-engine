package com.flashsale.repository;

import com.flashsale.model.Reservation;
import com.flashsale.model.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByReservationToken(String reservationToken);

    Optional<Reservation> findByIdempotencyKey(String idempotencyKey);

    List<Reservation> findByStatusAndExpiresAtBefore(ReservationStatus status, LocalDateTime cutoffTime);

    long countByTicketTierIdAndStatus(Long ticketTierId, ReservationStatus status);
}
