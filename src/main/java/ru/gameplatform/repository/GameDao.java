package ru.gameplatform.repository;

import ru.gameplatform.config.DatabaseConfig;
import ru.gameplatform.model.Game;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GameDao {

    public List<Game> findAllActive() {
        String sql = """
                SELECT id, name, type, description, min_bet, max_bet, is_active
                FROM game
                WHERE is_active = TRUE
                ORDER BY id
                """;

        List<Game> games = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                games.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка выборки игр из базы данных", e);
        }

        return games;
    }

    public Optional<Game> findByType(String type) {
        String sql = """
                SELECT id, name, type, description, min_bet, max_bet, is_active
                FROM game
                WHERE type = ? AND is_active = TRUE
                """;
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска игры по типу", e);
        }
        return Optional.empty();
    }

    private Game mapRow(ResultSet rs) throws SQLException {
        return new Game(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("type"),
                rs.getString("description"),
                rs.getBigDecimal("min_bet"),
                rs.getBigDecimal("max_bet"),
                rs.getBoolean("is_active")
        );
    }
}