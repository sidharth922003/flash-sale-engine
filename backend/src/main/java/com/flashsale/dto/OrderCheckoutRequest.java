package com.flashsale.dto;

import jakarta.validation.constraints.NotBlank;

public record OrderCheckoutRequest(
    @NotBlank(message = "Reservation token is required")
    String reservationToken,

    @NotBlank(message = "Payment method is required")
    String paymentMethod,

    String paymentReference
) {}
