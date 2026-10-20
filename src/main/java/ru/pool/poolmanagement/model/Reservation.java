package ru.pool.poolmanagement.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Reservation {
    private UUID id;
    private Long clientId;
    private LocalDateTime reservedTime;
    private ReservationStatus status;
    private String cancelReason;
    private LocalDateTime createdAt;

    public Reservation() {
    }

    public Reservation(UUID id, Long clientId, LocalDateTime reservedTime, ReservationStatus status,
                       String cancelReason, LocalDateTime createdAt) {
        this.id = id;
        this.clientId = clientId;
        this.reservedTime = reservedTime;
        this.status = status;
        this.cancelReason = cancelReason;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public LocalDateTime getReservedTime() {
        return reservedTime;
    }

    public void setReservedTime(LocalDateTime reservedTime) {
        this.reservedTime = reservedTime;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}