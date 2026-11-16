package ru.gameplatform.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UserRoleDao {

    public void assign(Connection conn, int userId, String roleName) throws SQLException {
        String sql = """
                INSERT INTO user_role (user_id, role_id)
                VALUES (?, (SELECT id FROM role WHERE name = ?))
                ON CONFLICT DO NOTHING
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, roleName);
            ps.executeUpdate();
        }
    }
}