package com.flashsale.dto;

import com.flashsale.model.enums.ConcurrencyStrategy;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReservationRequest(
    @NotNull(message = "Ticket tier ID is required")
    Long ticketTierId,

    @Min(value = 1, message = "Minimum 1 ticket required")
    @Max(value = 4, message = "Maximum 4 tickets per transaction")
    Integer quantity,

    @NotBlank(message = "User email is required")
    @Email(message = "Valid email address required")
    String userEmail,

    @NotBlank(message = "Idempotency key is required to prevent double-charging")
    String idempotencyKey,

    ConcurrencyStrategy strategy
) {}
