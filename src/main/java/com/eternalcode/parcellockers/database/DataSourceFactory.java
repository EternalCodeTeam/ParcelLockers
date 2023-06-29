package com.eternalcode.parcellockers.database;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;

public class DataSourceFactory {

    private DataSourceFactory() {
    }

    public static HikariDataSource buildHikariDataSource(PluginConfiguration databaseConfig, File dataFolder) {
        HikariConfig hikariConfig = new HikariConfig();

        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", true);

        PluginConfiguration.Settings settings = databaseConfig.settings;
        switch (settings.databaseType) {
            case MYSQL -> {
                hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
                hikariConfig.setJdbcUrl("jdbc:mysql://" + settings.host + ":" + settings.port + "/" + settings.databaseName + "?useSSL=" + settings.useSSL);
                hikariConfig.setUsername(settings.user);
                hikariConfig.setPassword(settings.password);
            }

            case SQLITE -> {
                hikariConfig.setDriverClassName("org.sqlite.JDBC");
                hikariConfig.setJdbcUrl("jdbc:sqlite:" + dataFolder + "/database.db");
            }
            default -> throw new IllegalStateException("Unexpected value: " + settings.databaseType);
        }

        return new HikariDataSource(hikariConfig);
    }

}
