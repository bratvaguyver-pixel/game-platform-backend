package ru.gameplatform.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class GameSettingsRepository {

    public Optional<String> findValue(Connection conn, int gameId, String parameterName) throws SQLException {
        String sql = "SELECT parameter_value FROM game_settings WHERE game_id = ? AND parameter_name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, gameId);
            ps.setString(2, parameterName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getString("parameter_value"));
                }
            }
        }
        return Optional.empty();
    }
}