package com.flashsale.dto;

import com.flashsale.model.enums.ConcurrencyStrategy;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StressTestRequest(
    @NotNull(message = "Ticket Tier ID is required")
    Long ticketTierId,

    @Min(value = 10, message = "Minimum 10 concurrent requests")
    @Max(value = 2000, message = "Maximum 2000 concurrent requests in demo mode")
    Integer totalConcurrentUsers,

    @Min(value = 1, message = "Minimum 1 ticket per user")
    @Max(value = 4, message = "Maximum 4 tickets per user")
    Integer quantityPerUser,

    ConcurrencyStrategy strategy
) {}
