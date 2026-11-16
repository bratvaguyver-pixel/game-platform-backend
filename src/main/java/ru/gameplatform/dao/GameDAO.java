package ru.gameplatform.dao;

import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;
import ru.gameplatform.model.Game;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class GameDAO {

    private final DataSource dataSource;

    public GameDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Game> findAllActive() {
        String sql = """
                SELECT id, name, type, description, min_bet, max_bet, is_active
                FROM game
                WHERE is_active = TRUE
                ORDER BY id
                """;
        List<Game> games = new ArrayList<>();
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                games.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка выборки игр из базы данных", e);
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }
        return games;
    }

    public Optional<Game> findByType(String type) {
        String sql = """
                SELECT id, name, type, description, min_bet, max_bet, is_active
                FROM game
                WHERE type = ? AND is_active = TRUE
                """;
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска игры по типу", e);
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
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