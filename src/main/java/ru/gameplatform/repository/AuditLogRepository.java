package ru.gameplatform.repository;

import ru.gameplatform.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class AuditLogRepository {

    public void log(String action, Integer userId, String detailsJson, String ip) {
        String sql = """
                INSERT INTO audit_log (user_id, action, details, ip_address)
                VALUES (?, ?, ?::jsonb, ?)
                """;
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (userId == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, userId);
            }
            ps.setString(2, action);
            ps.setString(3, detailsJson);
            ps.setString(4, ip);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Не удалось записать audit_log: " + e.getMessage());
        }
    }
}