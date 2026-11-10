package ru.gameplatform.util;

import ru.gameplatform.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.SQLException;

public class JdbcTransaction {

    @FunctionalInterface
    public interface TxBody<T> {
        T run(Connection conn) throws SQLException;
    }

    public static <T> T execute(TxBody<T> body) {
        Connection conn = null;
        try {
            conn = DatabaseConfig.getDataSource().getConnection();
            conn.setAutoCommit(false);
            T result = body.run(conn);
            conn.commit();
            return result;
        } catch (SQLException e) {
            rollback(conn);
            throw new RuntimeException("Ошибка транзакции: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            rollback(conn);
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    private static void rollback(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {
            }
        }
    }
}