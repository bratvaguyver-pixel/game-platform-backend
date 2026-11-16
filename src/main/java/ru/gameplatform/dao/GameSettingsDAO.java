package ru.gameplatform.dao;

import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Repository
public class GameSettingsDAO {

    private final DataSource dataSource;

    public GameSettingsDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Optional<String> findValue(int gameId, String parameterName) {
        String sql = "SELECT parameter_value FROM game_settings WHERE game_id = ? AND parameter_name = ?";
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, gameId);
            ps.setString(2, parameterName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getString("parameter_value"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка чтения настроек игры", e);
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }
        return Optional.empty();
    }
}