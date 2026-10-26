package com.flashsale.service;

import com.flashsale.dto.OrderCheckoutRequest;
import com.flashsale.dto.OrderResponse;
import com.flashsale.model.Order;
import com.flashsale.model.Reservation;
import com.flashsale.model.enums.OrderStatus;
import com.flashsale.model.enums.ReservationStatus;
import com.flashsale.repository.OrderRepository;
import com.flashsale.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class OrderService {

    private final ReservationRepository reservationRepository;
    private final OrderRepository orderRepository;

    public OrderService(ReservationRepository reservationRepository, OrderRepository orderRepository) {
        this.reservationRepository = reservationRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public OrderResponse checkout(OrderCheckoutRequest request) {
        Reservation reservation = reservationRepository.findByReservationToken(request.reservationToken())
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            Order existing = orderRepository.findByReservationId(reservation.getId())
                    .orElseThrow(() -> new IllegalStateException("Order already confirmed"));
            return mapToResponse(existing);
        }

        if (reservation.isExpired() || reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException("Reservation hold has expired or is invalid. Seats have been released.");
        }

        // Finalize state
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservationRepository.save(reservation);

        String orderNumber = "ORD-" + LocalDateTime.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String paymentRef = request.paymentReference() != null ? request.paymentReference() : "PAY-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();

        Order order = new Order(
                orderNumber,
                reservation,
                reservation.getTotalAmount(),
                paymentRef
        );

        Order saved = orderRepository.save(order);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderNumber));
        return mapToResponse(order);
    }

    private OrderResponse mapToResponse(Order order) {
        Reservation r = order.getReservation();
        return new OrderResponse(
                order.getOrderNumber(),
                r.getReservationToken(),
                r.getTicketTier().getEvent().getTitle(),
                r.getTicketTier().getName(),
                r.getQuantity(),
                order.getAmountPaid(),
                order.getStatus(),
                order.getPaymentReference(),
                order.getConfirmedAt()
        );
    }
}
