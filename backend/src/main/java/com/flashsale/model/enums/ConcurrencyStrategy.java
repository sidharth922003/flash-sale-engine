package com.flashsale.model.enums;

public enum ConcurrencyStrategy {
    ATOMIC_DB_CONDITIONAL, // Atomic SQL UPDATE with available_seats >= requested (Best throughput)
    PESSIMISTIC_LOCK,      // SELECT ... FOR UPDATE (Strict row lock)
    OPTIMISTIC_LOCK,       // JPA @Version row versioning (Retry on conflict)
    STRIPED_LOCK           // Distributed / In-Memory striped mutex lock
}
