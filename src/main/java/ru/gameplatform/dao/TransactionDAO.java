package ru.gameplatform.dao;

import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;
import ru.gameplatform.dto.PageResponse;
import ru.gameplatform.dto.TransactionHistoryItem;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class TransactionDAO {

    private final DataSource dataSource;

    public TransactionDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void insert(int userId, BigDecimal amount, String type, String description) {
        String sql = """
                INSERT INTO transaction (user_id, amount, type, description)
                VALUES (?, ?, ?, ?)
                """;
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setBigDecimal(2, amount);
            ps.setString(3, type);
            ps.setString(4, description);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка записи транзакции", e);
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }
    }

    public PageResponse<TransactionHistoryItem> findHistory(int userId, String type, String from, String to,
                                                            int page, int size) {
        StringBuilder where = new StringBuilder(" WHERE t.user_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(userId);

        if (type != null && !type.isBlank()) {
            where.append(" AND t.type = ?");
            params.add(type.toUpperCase());
        }
        if (from != null && !from.isBlank()) {
            where.append(" AND t.created_at >= ?::date");
            params.add(from);
        }
        if (to != null && !to.isBlank()) {
            where.append(" AND t.created_at < (?::date + INTERVAL '1 day')");
            params.add(to);
        }

        String base = """
                FROM transaction t
                """;

        String countSql = "SELECT COUNT(*)\n" + base + where;

        String selectSql = "SELECT t.id, t.amount, t.type, t.description, t.created_at\n"
                + base + where
                + "\nORDER BY t.created_at DESC, t.id DESC\nLIMIT ? OFFSET ?";

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

            List<TransactionHistoryItem> content = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                int idx = 1;
                for (Object p : params) {
                    ps.setObject(idx++, p);
                }
                ps.setInt(idx++, size);
                ps.setInt(idx, page * size);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        content.add(new TransactionHistoryItem(
                                rs.getInt("id"),
                                rs.getBigDecimal("amount"),
                                rs.getString("type"),
                                rs.getString("description"),
                                rs.getTimestamp("created_at").toLocalDateTime().toString()
                        ));
                    }
                }
            }

            int totalPages = (int) Math.ceil((double) total / size);
            return new PageResponse<>(content, page, size, total, totalPages);

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка выборки истории транзакций", e);
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }
    }
}