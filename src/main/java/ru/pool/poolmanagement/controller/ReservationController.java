package ru.pool.poolmanagement.controller;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.pool.poolmanagement.dto.reservation.AvailableSlotResponse;
import ru.pool.poolmanagement.dto.reservation.CancelReservationRequest;
import ru.pool.poolmanagement.dto.reservation.CreateReservationRequest;
import ru.pool.poolmanagement.dto.reservation.OccupiedSlotResponse;
import ru.pool.poolmanagement.dto.reservation.ReservationIdResponse;
import ru.pool.poolmanagement.service.ReservationService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v0/pool")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    // Получение доступных записей на определенную дату
    @GetMapping("/time-slots/available")
    public List<AvailableSlotResponse> getAvailableSlots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return reservationService.getAvailableSlots(date);
    }

    // Получение занятых записей на определенную дату
    @GetMapping("/reservations")
    public List<OccupiedSlotResponse> getOccupiedSlots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return reservationService.getOccupiedSlots(date);
    }

    // Добавить запись клиента на определенное время
    @PostMapping("/reservations")
    public ResponseEntity<ReservationIdResponse> createReservation(
            @Valid @RequestBody CreateReservationRequest request) {
        UUID reservationId = reservationService.bookReservation(
                request.clientId(),
                request.datetime(),
                request.durationHours()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ReservationIdResponse(reservationId.toString()));
    }

    // Отмена записи клиента на определенное время
    @PostMapping("/reservations/{id}/cancel")
    public ResponseEntity<Void> cancelReservation(
            @PathVariable UUID id,
            @Valid @RequestBody CancelReservationRequest request) {
        reservationService.cancelReservation(id, request.clientId(), request.reason());
        return ResponseEntity.ok().build();
    }
}