package com.eternalcode.parcellockers.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.testcontainers.containers.MySQLContainer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

class ParcelLockerIntegrationSpec {

    <T> T await(CompletableFuture<T> future) {
        return future
            .orTimeout(5, TimeUnit.SECONDS)
            .join();
    }

    static HikariDataSource buildHikariDataSource(MySQLContainer mySQLContainer) {
        HikariConfig hikariConfig = new HikariConfig();

        hikariConfig.setDriverClassName(mySQLContainer.getDriverClassName());
        hikariConfig.setJdbcUrl(mySQLContainer.getJdbcUrl());
        hikariConfig.setUsername(mySQLContainer.getUsername());
        hikariConfig.setPassword(mySQLContainer.getPassword());

        return new HikariDataSource(hikariConfig);
    }
}
