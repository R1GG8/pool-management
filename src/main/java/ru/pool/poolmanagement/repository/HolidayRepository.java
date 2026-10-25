package ru.pool.poolmanagement.repository;

import org.springframework.stereotype.Repository;
import ru.pool.poolmanagement.model.HolidaySchedule;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

@Repository
public class HolidayRepository {

    private final DataSource dataSource;

    public HolidayRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Optional<HolidaySchedule> findByDate(LocalDate date) {
        String sql = "SELECT date, is_closed, open_time, close_time FROM holidays WHERE date = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(date));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    LocalTime open = rs.getTime("open_time") != null ? rs.getTime("open_time").toLocalTime() : null;
                    LocalTime close = rs.getTime("close_time") != null ? rs.getTime("close_time").toLocalTime() : null;

                    return Optional.of(new HolidaySchedule(
                            rs.getDate("date").toLocalDate(),
                            rs.getBoolean("is_closed"),
                            open,
                            close
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска праздничного расписания на дату " + date, e);
        }
        return Optional.empty();
    }
}