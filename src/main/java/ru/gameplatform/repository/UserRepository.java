package ru.gameplatform.repository;

import ru.gameplatform.config.DatabaseConfig;
import ru.gameplatform.model.User;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class UserRepository {

    public Optional<User> findByEmail(String email) {
        String sql = """
                SELECT u.id, u.username, u.email, u.password_hash, u.balance, u.is_active, r.name AS role
                FROM "user" u
                JOIN role r ON r.id = u.role_id
                WHERE u.email = ?
                """;
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска пользователя", e);
        }
        return Optional.empty();
    }

    public Optional<User> findById(int id) {
        String sql = """
                SELECT u.id, u.username, u.email, u.password_hash, u.balance, u.is_active, r.name AS role
                FROM "user" u
                JOIN role r ON r.id = u.role_id
                WHERE u.id = ?
                """;
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска пользователя по id", e);
        }
        return Optional.empty();
    }

    public boolean existsByUsername(String username) {
        String sql = "SELECT 1 FROM \"user\" WHERE username = ?";
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка проверки имени пользователя", e);
        }
    }

    public int insert(Connection conn, String username, String email, String passwordHash,
                      BigDecimal balance, String roleName) throws SQLException {
        String sql = """
                INSERT INTO "user" (username, email, password_hash, balance, role_id)
                VALUES (?, ?, ?, ?, (SELECT id FROM role WHERE name = ?))
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, passwordHash);
            ps.setBigDecimal(4, balance);
            ps.setString(5, roleName);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Не удалось получить id созданного пользователя");
    }

    public boolean decreaseBalance(Connection conn, int userId, BigDecimal amount) throws SQLException {
        String sql = "UPDATE \"user\" SET balance = balance - ? WHERE id = ? AND balance >= ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setInt(2, userId);
            ps.setBigDecimal(3, amount);
            return ps.executeUpdate() == 1;
        }
    }

    public void increaseBalance(Connection conn, int userId, BigDecimal amount) throws SQLException {
        String sql = "UPDATE \"user\" SET balance = balance + ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public BigDecimal getBalance(Connection conn, int userId) throws SQLException {
        String sql = "SELECT balance FROM \"user\" WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("balance");
                }
            }
        }
        throw new SQLException("Пользователь не найден: id=" + userId);
    }

    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getBigDecimal("balance"),
                rs.getString("role"),
                rs.getBoolean("is_active")
        );
    }
}