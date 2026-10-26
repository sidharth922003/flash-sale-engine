package com.flashsale.dto;

import com.flashsale.model.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderResponse(
    String orderNumber,
    String reservationToken,
    String eventTitle,
    String tierName,
    Integer quantity,
    BigDecimal amountPaid,
    OrderStatus status,
    String paymentReference,
    LocalDateTime confirmedAt
) {}
