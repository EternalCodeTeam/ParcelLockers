package com.eternalcode.parcellockers.database;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfiguration;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;

public class DataSourceBuilder {

    public HikariDataSource buildHikariDataSource(PluginConfiguration databaseConfig, File dataFolder) {
        HikariConfig hikariConfig = new HikariConfig();

        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", true);

        switch (databaseConfig.settings.databaseType) {
            case MYSQL -> {
                hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
                hikariConfig.setJdbcUrl("jdbc:mysql://" + databaseConfig.settings.host + ":" + databaseConfig.settings.port + "/" + databaseConfig.settings.databaseName);
                hikariConfig.setUsername(databaseConfig.settings.user);
                hikariConfig.setPassword(databaseConfig.settings.password);
            }

            case SQLITE -> {
                hikariConfig.setDriverClassName("org.sqlite.JDBC");
                hikariConfig.setJdbcUrl("jdbc:sqlite:" + dataFolder + "/database.db");
            }
        }

        return new HikariDataSource(hikariConfig);
    }

}
