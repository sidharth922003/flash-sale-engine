package com.flashsale.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Striped Lock Service
 * Provides fine-grained concurrency control partitioned across hash buckets.
 * Mimics distributed Redis key locks (e.g. Redisson RLock) in local memory.
 */
@Service
public class ConcurrencyLockService {

    private static final int STRIPE_COUNT = 64;
    private final ReentrantLock[] locks = new ReentrantLock[STRIPE_COUNT];

    public ConcurrencyLockService() {
        for (int i = 0; i < STRIPE_COUNT; i++) {
            locks[i] = new ReentrantLock(true); // fair lock
        }
    }

    private ReentrantLock getLockForTier(Long tierId) {
        int index = Math.abs(tierId.hashCode() % STRIPE_COUNT);
        return locks[index];
    }

    public boolean tryLock(Long tierId, long timeoutMs) {
        try {
            return getLockForTier(tierId).tryLock(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public void unlock(Long tierId) {
        ReentrantLock lock = getLockForTier(tierId);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
