package com.flashsale.model;

import com.flashsale.model.enums.ReservationStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations", indexes = {
    @Index(name = "idx_res_token", columnList = "reservationToken", unique = true),
    @Index(name = "idx_res_idempotency", columnList = "idempotencyKey", unique = true),
    @Index(name = "idx_res_status_expires", columnList = "status, expiresAt")
})
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String reservationToken;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ticket_tier_id", nullable = false)
    private TicketTier ticketTier;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReservationStatus status = ReservationStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    public Reservation() {}

    public Reservation(String reservationToken, TicketTier ticketTier, String userEmail, Integer quantity,
                       BigDecimal totalAmount, String idempotencyKey, LocalDateTime expiresAt) {
        this.reservationToken = reservationToken;
        this.ticketTier = ticketTier;
        this.userEmail = userEmail;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
        this.status = ReservationStatus.PENDING;
    }

    public boolean isExpired() {
        return this.status == ReservationStatus.PENDING && LocalDateTime.now().isAfter(this.expiresAt);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReservationToken() { return reservationToken; }
    public void setReservationToken(String reservationToken) { this.reservationToken = reservationToken; }

    public TicketTier getTicketTier() { return ticketTier; }
    public void setTicketTier(TicketTier ticketTier) { this.ticketTier = ticketTier; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
