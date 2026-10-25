package ru.pool.poolmanagement.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.pool.poolmanagement.exception.BadRequestException;
import ru.pool.poolmanagement.exception.SlotFullException;
import ru.pool.poolmanagement.model.Client;
import ru.pool.poolmanagement.repository.ClientRepository;
import ru.pool.poolmanagement.repository.ReservationRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ClientRepository clientRepository;

    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        reservationService = new ReservationService(
                reservationRepository,
                clientRepository,
                10,
                "08:00",
                "22:00"
        );
    }

    @Test
    @DisplayName("Успешное бронирование на 1 час (длительность по умолчанию)")
    void bookReservation_DefaultDuration_Success() {
        Long clientId = 1L;
        LocalDateTime futureTime = LocalDate.now().plusDays(1).atTime(12, 0, 0);

        when(clientRepository.findById(clientId))
                .thenReturn(Optional.of(new Client(clientId, "Иван", "+79991112233", "ivan@mail.ru")));
        when(reservationRepository.hasActiveReservationOnDate(eq(clientId), any(LocalDate.class)))
                .thenReturn(false);
        when(reservationRepository.bookConsecutiveSlotsWithLock(eq(clientId), eq(futureTime), eq(1), eq(10)))
                .thenReturn(UUID.randomUUID());

        UUID resultId = reservationService.bookReservation(clientId, futureTime, null);

        assertNotNull(resultId);
        verify(reservationRepository, times(1)).bookConsecutiveSlotsWithLock(clientId, futureTime, 1, 10);
    }

    @Test
    @DisplayName("Успешное бронирование на 2 часа подряд")
    void bookReservation_ConsecutiveHours_Success() {
        Long clientId = 1L;
        LocalDateTime futureTime = LocalDate.now().plusDays(1).atTime(10, 0, 0);

        when(clientRepository.findById(clientId))
                .thenReturn(Optional.of(new Client(clientId, "Иван", "+79991112233", "ivan@mail.ru")));
        when(reservationRepository.hasActiveReservationOnDate(eq(clientId), any(LocalDate.class)))
                .thenReturn(false);
        when(reservationRepository.bookConsecutiveSlotsWithLock(eq(clientId), eq(futureTime), eq(2), eq(10)))
                .thenReturn(UUID.randomUUID());

        UUID resultId = reservationService.bookReservation(clientId, futureTime, 2);

        assertNotNull(resultId);
        verify(reservationRepository, times(1)).bookConsecutiveSlotsWithLock(clientId, futureTime, 2, 10);
    }

    @Test
    @DisplayName("Ошибка, если длительность сеанса выходит за время закрытия бассейна")
    void bookReservation_ExceedsClosingTime_ThrowsBadRequest() {
        Long clientId = 1L;
        LocalDateTime lateTime = LocalDate.now().plusDays(1).atTime(21, 0, 0);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                reservationService.bookReservation(clientId, lateTime, 2)
        );

        assertTrue(ex.getMessage().contains("выходит за рамки рабочего времени"));
    }

    @Test
    @DisplayName("Ошибка, если время не кратно 1 часу (дискретность)")
    void bookReservation_NonDiscreteHour_ThrowsBadRequest() {
        Long clientId = 1L;
        LocalDateTime invalidTime = LocalDate.now().plusDays(1).atTime(12, 15, 0);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                reservationService.bookReservation(clientId, invalidTime, 1)
        );

        assertTrue(ex.getMessage().contains("кратно ровно 1 часу"));
    }

    @Test
    @DisplayName("Ошибка, если все места в слоте заняты (SlotFullException)")
    void bookReservation_CapacityExceeded_ThrowsSlotFullException() {
        Long clientId = 1L;
        LocalDateTime futureTime = LocalDate.now().plusDays(1).atTime(14, 0, 0);

        when(clientRepository.findById(clientId))
                .thenReturn(Optional.of(new Client(clientId, "Иван", "+79991112233", "ivan@mail.ru")));
        when(reservationRepository.hasActiveReservationOnDate(eq(clientId), any(LocalDate.class)))
                .thenReturn(false);
        when(reservationRepository.bookConsecutiveSlotsWithLock(any(), any(), anyInt(), anyInt()))
                .thenThrow(new IllegalStateException("В слоте уже нет свободных мест"));

        assertThrows(SlotFullException.class, () ->
                reservationService.bookReservation(clientId, futureTime, 1)
        );
    }

    @Test
    @DisplayName("Ошибка, если клиент уже записан на этот же день (лимит 1 визит в день)")
    void bookReservation_AlreadyBookedToday_ThrowsBadRequest() {
        Long clientId = 1L;
        LocalDateTime futureTime = LocalDate.now().plusDays(1).atTime(10, 0, 0);

        when(clientRepository.findById(clientId))
                .thenReturn(Optional.of(new Client(clientId, "Иван", "+79991112233", "ivan@mail.ru")));
        when(reservationRepository.hasActiveReservationOnDate(eq(clientId), eq(futureTime.toLocalDate())))
                .thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                reservationService.bookReservation(clientId, futureTime, 1)
        );

        assertTrue(ex.getMessage().contains("не более 1 посещения в день"));
        verify(reservationRepository, never()).bookConsecutiveSlotsWithLock(any(), any(), anyInt(), anyInt());
    }
}