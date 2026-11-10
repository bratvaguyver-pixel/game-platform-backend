package ru.gameplatform.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DatabaseConfig {

    private static HikariDataSource dataSource;

    public static synchronized HikariDataSource getDataSource() {
        if (dataSource == null) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(AppConfig.get("db.url"));
            config.setUsername(AppConfig.get("db.user"));
            config.setPassword(AppConfig.get("db.password"));
            config.setMaximumPoolSize(AppConfig.getInt("db.pool.size", 10));
            config.setConnectionTimeout(5000);
            dataSource = new HikariDataSource(config);
        }
        return dataSource;
    }

    public static void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}