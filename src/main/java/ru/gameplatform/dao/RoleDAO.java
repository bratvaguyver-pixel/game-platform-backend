package ru.gameplatform.dao;

import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Repository
public class RoleDAO {

    private final DataSource dataSource;

    public RoleDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void assign(int userId, String roleName) {
        String sql = """
                INSERT INTO user_role (user_id, role_id)
                VALUES (?, (SELECT id FROM role WHERE name = ?))
                ON CONFLICT DO NOTHING
                """;
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, roleName);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка назначения роли", e);
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }
    }
}