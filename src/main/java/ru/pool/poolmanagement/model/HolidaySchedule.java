package ru.pool.poolmanagement.model;

import java.time.LocalDate;
import java.time.LocalTime;

public class HolidaySchedule {
    private LocalDate date;
    private boolean closed;
    private LocalTime openTime;
    private LocalTime closeTime;

    public HolidaySchedule() {
    }

    public HolidaySchedule(LocalDate date, boolean closed, LocalTime openTime, LocalTime closeTime) {
        this.date = date;
        this.closed = closed;
        this.openTime = openTime;
        this.closeTime = closeTime;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public LocalTime getOpenTime() {
        return openTime;
    }

    public void setOpenTime(LocalTime openTime) {
        this.openTime = openTime;
    }

    public LocalTime getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(LocalTime closeTime) {
        this.closeTime = closeTime;
    }
}