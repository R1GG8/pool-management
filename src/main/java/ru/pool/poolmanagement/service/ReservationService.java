package ru.pool.poolmanagement.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.pool.poolmanagement.dto.reservation.AvailableSlotResponse;
import ru.pool.poolmanagement.dto.client.ClientDetailsResponse;
import ru.pool.poolmanagement.dto.reservation.OccupiedSlotResponse;
import ru.pool.poolmanagement.exception.BadRequestException;
import ru.pool.poolmanagement.exception.NotFoundException;
import ru.pool.poolmanagement.exception.SlotFullException;
import ru.pool.poolmanagement.model.Client;
import ru.pool.poolmanagement.model.Reservation;
import ru.pool.poolmanagement.repository.ClientRepository;
import ru.pool.poolmanagement.repository.ReservationRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ClientRepository clientRepository;

    private final int capacityPerHour;
    private final LocalTime openTime;
    private final LocalTime closeTime;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public ReservationService(
            ReservationRepository reservationRepository,
            ClientRepository clientRepository,
            @Value("${pool.capacity-per-hour:10}") int capacityPerHour,
            @Value("${pool.working-hours.open:08:00}") String openTime,
            @Value("${pool.working-hours.close:22:00}") String closeTime) {
        this.reservationRepository = reservationRepository;
        this.clientRepository = clientRepository;
        this.capacityPerHour = capacityPerHour;
        this.openTime = LocalTime.parse(openTime);
        this.closeTime = LocalTime.parse(closeTime);
    }

    public List<AvailableSlotResponse> getAvailableSlots(LocalDate date) {
        Map<Integer, Integer> hourlyCounts = reservationRepository.countActiveReservationsByHourForDate(date);
        List<AvailableSlotResponse> result = new ArrayList<>();

        int startHour = openTime.getHour();
        // Если бассейн работает до 22:00, последний одночасовой слот начинается в 21:00
        int lastHour = closeTime.getHour() - 1;

        for (int hour = startHour; hour <= lastHour; hour++) {
            int booked = hourlyCounts.getOrDefault(hour, 0);
            int available = capacityPerHour - booked;
            if (available > 0) {
                String timeStr = String.format("%02d:00", hour);
                result.add(new AvailableSlotResponse(timeStr, available));
            }
        }
        return result;
    }

    public List<OccupiedSlotResponse> getOccupiedSlots(LocalDate date) {
        Map<LocalDateTime, List<Client>> occupiedMap = reservationRepository.findOccupiedSlotsWithClients(date);
        List<OccupiedSlotResponse> result = new ArrayList<>();

        List<LocalDateTime> sortedTimes = new ArrayList<>(occupiedMap.keySet());
        Collections.sort(sortedTimes);

        for (LocalDateTime time : sortedTimes) {
            List<Client> clients = occupiedMap.get(time);
            List<ClientDetailsResponse> clientDtos = clients.stream()
                    .map(c -> new ClientDetailsResponse(c.getId(), c.getName(), c.getPhone(), c.getEmail()))
                    .toList();

            result.add(new OccupiedSlotResponse(
                    time.toLocalTime().format(TIME_FORMATTER),
                    clients.size(),
                    clientDtos
            ));
        }
        return result;
    }

    public UUID bookReservation(Long clientId, LocalDateTime datetime, Integer requestedDuration) {
        int duration = (requestedDuration != null) ? requestedDuration : 1;

        validateReservationTime(datetime, duration);

        clientRepository.findById(clientId)
                .orElseThrow(() -> new NotFoundException("Клиент с id " + clientId + " не существует"));

        LocalDate bookingDate = datetime.toLocalDate();
        if (reservationRepository.hasActiveReservationOnDate(clientId, bookingDate)) {
            throw new BadRequestException("Клиент уже записан на сеанс в этот день. Разрешено не более 1 посещения в день.");
        }

        try {
            return reservationRepository.bookConsecutiveSlotsWithLock(clientId, datetime, duration, capacityPerHour);
        } catch (IllegalStateException e) {
            throw new SlotFullException(e.getMessage());
        }
    }

    private void validateReservationTime(LocalDateTime startDatetime, int durationHours) {
        if (startDatetime.getMinute() != 0 || startDatetime.getSecond() != 0 || startDatetime.getNano() != 0) {
            throw new BadRequestException("Время записи должно быть кратно ровно 1 часу (например, 10:00, 11:00)");
        }

        if (startDatetime.isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Нельзя записаться на прошедшее время");
        }

        LocalTime startTime = startDatetime.toLocalTime();
        LocalTime endTime = startTime.plusHours(durationHours);

        // Проверяем, не начинается ли запись до открытия
        if (startTime.isBefore(openTime)) {
            throw new BadRequestException("Бассейн открывается в " + openTime);
        }

        if (endTime.isAfter(closeTime) || (endTime.isBefore(startTime) && !closeTime.equals(LocalTime.MIDNIGHT))) {
            throw new BadRequestException(String.format(
                    "Сеанс длительностью %d ч. выходит за рамки рабочего времени бассейна (закрытие в %s)",
                    durationHours, closeTime
            ));
        }
    }

    public void cancelReservation(UUID reservationId, Long clientId, String reason) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("Запись с id " + reservationId + " не найдена"));

        if (!reservation.getClientId().equals(clientId)) {
            throw new BadRequestException("Указанный clientId не совпадает с владельцем записи");
        }

        if ("CANCELLED".equals(reservation.getStatus())) {
            throw new BadRequestException("Эта запись уже была отменена ранее");
        }

        reservationRepository.cancelReservation(reservationId, reason);
    }
}