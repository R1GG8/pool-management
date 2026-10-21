package ru.pool.poolmanagement.repository;

import org.springframework.stereotype.Repository;
import ru.pool.poolmanagement.model.Client;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class ClientRepository {

    private final DataSource dataSource;

    public ClientRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Client> findAll() {
        String sql = "SELECT id, name, phone, email FROM clients ORDER BY id";
        List<Client> clients = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                clients.add(mapRowToClient(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при получении списка клиентов", e);
        }
        return clients;
    }

    public Optional<Client> findById(Long id) {
        String sql = "SELECT id, name, phone, email FROM clients WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToClient(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при поиске клиента с id " + id, e);
        }
        return Optional.empty();
    }

    public Client save(Client client) {
        String sql = "INSERT INTO clients (name, phone, email) VALUES (?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, client.getName());
            ps.setString(2, client.getPhone());
            ps.setString(3, client.getEmail());

            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    client.setId(generatedKeys.getLong(1));
                }
            }
            return client;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при сохранении клиента", e);
        }
    }

    public void update(Client client) {
        String sql = "UPDATE clients SET name = ?, phone = ?, email = ? WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, client.getName());
            ps.setString(2, client.getPhone());
            ps.setString(3, client.getEmail());
            ps.setLong(4, client.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при обновлении данных клиента", e);
        }
    }

    public List<Client> findByNameContaining(String namePart) {
        String sql = "SELECT id, name, phone, email FROM clients WHERE LOWER(name) LIKE LOWER(?) ORDER BY id";
        List<Client> clients = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + namePart + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    clients.add(mapRowToClient(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при поиске клиентов по имени", e);
        }
        return clients;
    }

    private Client mapRowToClient(ResultSet rs) throws SQLException {
        return new Client(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("phone"),
                rs.getString("email")
        );
    }
}