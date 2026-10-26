package com.flashsale.scheduler;

import com.flashsale.service.ReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReservationCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservationCleanupScheduler.class);
    private final ReservationService reservationService;

    public ReservationCleanupScheduler(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Scheduled(fixedDelayString = "${flashsale.reservation.cleanup-interval-ms:30000}")
    public void cleanupExpiredReservations() {
        try {
            int released = reservationService.releaseExpiredReservations();
            if (released > 0) {
                log.info("Auto-released {} expired ticket reservations back into the inventory pool.", released);
            }
        } catch (Exception e) {
            log.error("Error running reservation cleanup scheduler: {}", e.getMessage());
        }
    }
}
