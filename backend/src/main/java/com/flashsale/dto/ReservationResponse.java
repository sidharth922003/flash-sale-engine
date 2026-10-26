package com.flashsale.dto;

import com.flashsale.model.enums.ReservationStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservationResponse(
    String reservationToken,
    Long eventId,
    String eventTitle,
    Long ticketTierId,
    String tierName,
    Integer quantity,
    BigDecimal unitPrice,
    BigDecimal totalAmount,
    ReservationStatus status,
    LocalDateTime createdAt,
    LocalDateTime expiresAt,
    long remainingSeconds
) {}
