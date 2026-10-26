package com.flashsale.dto;

import com.flashsale.model.enums.ConcurrencyStrategy;

public record StressTestResult(
    Long tierId,
    String tierName,
    int initialAvailableSeats,
    int finalAvailableSeats,
    int totalRequests,
    int successfulReservations,
    int rejectedRequests,
    int totalSeatsSold,
    int oversoldCount, // Crucial for interview demo: MUST BE 0!
    long executionDurationMs,
    double requestsPerSecond,
    ConcurrencyStrategy strategyUsed,
    String statusMessage
) {}
