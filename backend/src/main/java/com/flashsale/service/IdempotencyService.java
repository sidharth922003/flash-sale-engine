package com.flashsale.service;

import com.flashsale.model.Reservation;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Idempotency Key Manager
 * Detects duplicate request submissions within the flash sale window.
 */
@Service
public class IdempotencyService {

    private final ConcurrentHashMap<String, Long> activeKeys = new ConcurrentHashMap<>();

    public boolean registerKey(String idempotencyKey, Long reservationId) {
        return activeKeys.putIfAbsent(idempotencyKey, reservationId) == null;
    }

    public Long getReservationIdForKey(String idempotencyKey) {
        return activeKeys.get(idempotencyKey);
    }

    public void removeKey(String idempotencyKey) {
        activeKeys.remove(idempotencyKey);
    }
}
