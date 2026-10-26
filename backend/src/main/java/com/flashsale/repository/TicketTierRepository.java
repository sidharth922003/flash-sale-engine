package com.flashsale.repository;

import com.flashsale.model.TicketTier;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TicketTierRepository extends JpaRepository<TicketTier, Long> {

    /**
     * ATOMIC CONDITIONAL SQL DECREMENT
     * Directly decrements available seats in a single atomic SQL statement
     * IF AND ONLY IF availableSeats >= requested quantity.
     * Guarantees ZERO overselling with maximum database concurrency.
     */
    @Modifying
    @Query("UPDATE TicketTier t SET t.availableSeats = t.availableSeats - :quantity WHERE t.id = :tierId AND t.availableSeats >= :quantity")
    int decrementAvailableSeatsAtomic(@Param("tierId") Long tierId, @Param("quantity") int quantity);

    /**
     * ATOMIC RESTORATION
     * Returns seats back to the tier when a reservation expires or is cancelled.
     */
    @Modifying
    @Query("UPDATE TicketTier t SET t.availableSeats = t.availableSeats + :quantity WHERE t.id = :tierId AND (t.availableSeats + :quantity) <= t.totalSeats")
    int incrementAvailableSeatsAtomic(@Param("tierId") Long tierId, @Param("quantity") int quantity);

    /**
     * PESSIMISTIC LOCK (SELECT ... FOR UPDATE)
     * Acquires exclusive row-level lock on the tier row for strict serial execution.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TicketTier t WHERE t.id = :tierId")
    Optional<TicketTier> findByIdWithPessimisticLock(@Param("tierId") Long tierId);

    @Modifying
    @Query("UPDATE TicketTier t SET t.availableSeats = :seats WHERE t.id = :tierId")
    void setAvailableSeats(@Param("tierId") Long tierId, @Param("seats") int seats);
}
