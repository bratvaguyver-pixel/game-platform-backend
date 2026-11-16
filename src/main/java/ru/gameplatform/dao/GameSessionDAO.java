package ru.gameplatform.dao;

import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;
import ru.gameplatform.dto.PageResponse;
import ru.gameplatform.dto.SessionHistoryItem;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class GameSessionDAO {

    private final DataSource dataSource;

    public GameSessionDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void insert(int userId, int gameId, BigDecimal betAmount, String result, BigDecimal winAmount) {
        String sql = """
                INSERT INTO game_session (user_id, game_id, bet_amount, result, win_amount)
                VALUES (?, ?, ?, ?, ?)
                """;
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, gameId);
            ps.setBigDecimal(3, betAmount);
            ps.setString(4, result);
            ps.setBigDecimal(5, winAmount);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка записи игровой сессии", e);
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }
    }

    public PageResponse<SessionHistoryItem> findHistory(int userId, String gameType, String result,
                                                        int page, int size) {
        StringBuilder where = new StringBuilder(" WHERE gs.user_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(userId);

        if (gameType != null && !gameType.isBlank()) {
            where.append(" AND g.type = ?");
            params.add(gameType.toUpperCase());
        }
        if (result != null && !result.isBlank()) {
            where.append(" AND gs.result = ?");
            params.add(result.toUpperCase());
        }

        String base = """
                FROM game_session gs
                JOIN game g ON g.id = gs.game_id
                """;

        String countSql = "SELECT COUNT(*)\n" + base + where;

        String selectSql = """
                SELECT gs.id, g.name AS game_name, g.type AS game_type,
                       gs.bet_amount, gs.result, gs.win_amount, gs.created_at
                """ + base + where + """
                
                ORDER BY gs.created_at DESC, gs.id DESC
                LIMIT ? OFFSET ?
                """;

        Connection conn = DataSourceUtils.getConnection(dataSource);
        try {
            long total;
            try (PreparedStatement ps = conn.prepareStatement(countSql)) {
                for (int i = 0; i < params.size(); i++) {
                    ps.setObject(i + 1, params.get(i));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    total = rs.getLong(1);
                }
            }

            List<SessionHistoryItem> content = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                int idx = 1;
                for (Object p : params) {
                    ps.setObject(idx++, p);
                }
                ps.setInt(idx++, size);
                ps.setInt(idx, page * size);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        content.add(new SessionHistoryItem(
                                rs.getInt("id"),
                                rs.getString("game_name"),
                                rs.getString("game_type"),
                                rs.getBigDecimal("bet_amount"),
                                rs.getString("result"),
                                rs.getBigDecimal("win_amount"),
                                rs.getTimestamp("created_at").toLocalDateTime().toString()
                        ));
                    }
                }
            }

            int totalPages = (int) Math.ceil((double) total / size);
            return new PageResponse<>(content, page, size, total, totalPages);

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка выборки истории сессий", e);
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }
    }
}