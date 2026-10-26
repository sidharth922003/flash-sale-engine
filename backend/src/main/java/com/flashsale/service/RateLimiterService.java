package com.flashsale.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-Memory Token Bucket Rate Limiter
 * Ensures fairness and mitigates DDoS / automated scalper bot attacks.
 */
@Service
public class RateLimiterService {

    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final int capacity = 30; // max bursts allowed
    private final int refillRatePerSec = 10; // tokens added per second

    public boolean tryAcquire(String clientIdentifier) {
        TokenBucket bucket = buckets.computeIfAbsent(clientIdentifier, k -> new TokenBucket(capacity, refillRatePerSec));
        return bucket.tryConsume();
    }

    private static class TokenBucket {
        private final int capacity;
        private final int refillRate;
        private final AtomicInteger tokens;
        private volatile long lastRefillTimestamp;

        public TokenBucket(int capacity, int refillRate) {
            this.capacity = capacity;
            this.refillRate = refillRate;
            this.tokens = new AtomicInteger(capacity);
            this.lastRefillTimestamp = System.currentTimeMillis();
        }

        public synchronized boolean tryConsume() {
            refill();
            if (tokens.get() > 0) {
                tokens.decrementAndGet();
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long elapsedSeconds = (now - lastRefillTimestamp) / 1000;
            if (elapsedSeconds > 0) {
                int tokensToAdd = (int) (elapsedSeconds * refillRate);
                int current = tokens.get();
                tokens.set(Math.min(capacity, current + tokensToAdd));
                lastRefillTimestamp = now;
            }
        }
    }
}
