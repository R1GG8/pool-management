package ru.pool.poolmanagement.repository;

import org.springframework.stereotype.Repository;
import ru.pool.poolmanagement.model.Client;
import ru.pool.poolmanagement.model.Reservation;
import ru.pool.poolmanagement.model.ReservationStatus;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ReservationRepository {

    private final DataSource dataSource;

    public ReservationRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Map<Integer, Integer> countActiveReservationsByHourForDate(LocalDate date) {
        String sql = """
                    SELECT EXTRACT(HOUR FROM reserved_time) AS slot_hour, COUNT(*) AS booked_count
                    FROM reservations
                    WHERE reserved_time >= ? AND reserved_time < ? AND status = 'ACTIVE'
                    GROUP BY EXTRACT(HOUR FROM reserved_time)
                """;

        Map<Integer, Integer> hourlyCounts = new HashMap<>();
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(startOfDay));
            ps.setTimestamp(2, Timestamp.valueOf(endOfDay));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    hourlyCounts.put(rs.getInt("slot_hour"), rs.getInt("booked_count"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка подсчета слотов за дату " + date, e);
        }
        return hourlyCounts;
    }

    public Map<LocalDateTime, List<Client>> findOccupiedSlotsWithClients(LocalDate date) {
        String sql = """
                    SELECT r.reserved_time, c.id, c.name, c.phone, c.email
                    FROM reservations r
                    JOIN clients c ON r.client_id = c.id
                    WHERE r.reserved_time >= ? AND r.reserved_time < ? AND r.status = 'ACTIVE'
                    ORDER BY r.reserved_time, c.id
                """;

        Map<LocalDateTime, List<Client>> map = new HashMap<>();
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(startOfDay));
            ps.setTimestamp(2, Timestamp.valueOf(endOfDay));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDateTime time = rs.getTimestamp("reserved_time").toLocalDateTime();
                    Client client = new Client(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getString("phone"),
                            rs.getString("email")
                    );
                    map.computeIfAbsent(time, k -> new ArrayList<>()).add(client);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения занятых записей за дату", e);
        }
        return map;
    }

    public boolean hasActiveReservationOnDate(Long clientId, LocalDate date) {
        String sql = """
                    SELECT 1 FROM reservations
                    WHERE client_id = ? 
                      AND reserved_time >= ? 
                      AND reserved_time < ? 
                      AND status = 'ACTIVE'
                    LIMIT 1
                """;

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, clientId);
            ps.setTimestamp(2, Timestamp.valueOf(startOfDay));
            ps.setTimestamp(3, Timestamp.valueOf(endOfDay));

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка проверки записей клиента на день", e);
        }
    }

    public Optional<Reservation> findById(UUID id) {
        String sql = "SELECT id, client_id, reserved_time, status, cancel_reason, created_at FROM reservations WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Reservation(
                            rs.getObject("id", UUID.class),
                            rs.getLong("client_id"),
                            rs.getTimestamp("reserved_time").toLocalDateTime(),
                            ReservationStatus.valueOf(rs.getString("status")), // <-- Преобразуем строку из БД в Enum
                            rs.getString("cancel_reason"),
                            rs.getTimestamp("created_at").toLocalDateTime()
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска записи по ID", e);
        }
        return Optional.empty();
    }

    public void cancelReservation(UUID id, String reason) {
        // Находим бронь и отменяем её саму и все смежные часы, созданные в этой же транзакции
        String sql = """
                    UPDATE reservations 
                    SET status = 'CANCELLED', cancel_reason = ? 
                    WHERE (client_id, created_at) = (
                        SELECT client_id, created_at FROM reservations WHERE id = ?
                    ) AND status = 'ACTIVE'
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, reason);
            ps.setObject(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка отмены бронирования", e);
        }
    }

    // Атомарное бронирование N часов подряд с защитой от состояния гонки
    public UUID bookConsecutiveSlotsWithLock(Long clientId, LocalDateTime startTime, int durationHours, int maxCapacity) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Блокируем ВСЕ запрашиваемые часы (например, 10:00, 11:00, 12:00)
                String lockSql = "SELECT pg_advisory_xact_lock(hashtext(?))";
                try (PreparedStatement psLock = conn.prepareStatement(lockSql)) {
                    for (int i = 0; i < durationHours; i++) {
                        LocalDateTime slot = startTime.plusHours(i);
                        psLock.setString(1, "pool_slot_" + slot.toString());
                        psLock.execute();
                    }
                }

                // 2. Проверяем наличие мест в КАЖДОМ из этих часов
                String countSql = "SELECT COUNT(*) FROM reservations WHERE reserved_time = ? AND status = 'ACTIVE'";
                try (PreparedStatement psCount = conn.prepareStatement(countSql)) {
                    for (int i = 0; i < durationHours; i++) {
                        LocalDateTime slot = startTime.plusHours(i);
                        psCount.setTimestamp(1, Timestamp.valueOf(slot));
                        try (ResultSet rs = psCount.executeQuery()) {
                            if (rs.next() && rs.getInt(1) >= maxCapacity) {
                                throw new IllegalStateException("В слоте на " + slot.toLocalTime() + " уже нет свободных мест");
                            }
                        }
                    }
                }

                // 3. Создаем записи на каждый час
                UUID firstReservationId = null;
                String insertSql = "INSERT INTO reservations (id, client_id, reserved_time, status) VALUES (?, ?, ?, 'ACTIVE')";
                try (PreparedStatement psInsert = conn.prepareStatement(insertSql)) {
                    for (int i = 0; i < durationHours; i++) {
                        UUID newId = UUID.randomUUID();
                        if (i == 0) {
                            firstReservationId = newId; // ID первой записи вернем клиенту
                        }
                        LocalDateTime slot = startTime.plusHours(i);
                        psInsert.setObject(1, newId);
                        psInsert.setLong(2, clientId);
                        psInsert.setTimestamp(3, Timestamp.valueOf(slot));
                        psInsert.executeUpdate();
                    }
                }

                conn.commit();
                return firstReservationId;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка транзакции при бронировании: " + e.getMessage(), e);
        }
    }
}