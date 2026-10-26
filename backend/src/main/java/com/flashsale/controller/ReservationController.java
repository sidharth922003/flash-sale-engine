package com.flashsale.controller;

import com.flashsale.dto.ReservationRequest;
import com.flashsale.dto.ReservationResponse;
import com.flashsale.service.ReservationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping("/reserve")
    public ResponseEntity<ReservationResponse> reserveTickets(
            @Valid @RequestBody ReservationRequest request,
            HttpServletRequest servletRequest) {

        String clientIp = servletRequest.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = servletRequest.getRemoteAddr();
        }

        ReservationResponse response = reservationService.reserveTickets(request, clientIp);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{token}")
    public ResponseEntity<ReservationResponse> getReservation(@PathVariable String token) {
        return ResponseEntity.ok(reservationService.getReservation(token));
    }

    @DeleteMapping("/{token}")
    public ResponseEntity<Void> cancelReservation(@PathVariable String token) {
        reservationService.cancelReservation(token);
        return ResponseEntity.noContent().build();
    }
}
