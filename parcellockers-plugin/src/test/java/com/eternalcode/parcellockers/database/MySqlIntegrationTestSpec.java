package com.eternalcode.parcellockers.database;

import com.eternalcode.parcellockers.configuration.implementation.PluginConfig;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Shares a single MySQL container across all integration test classes (singleton container pattern).
 * The container is started lazily once per JVM and stopped by Testcontainers' Ryuk on JVM exit.
 * Every test starts with an empty schema, so test classes stay isolated from each other.
 */
@Testcontainers(disabledWithoutDocker = true)
abstract class MySqlIntegrationTestSpec extends IntegrationTestSpec {

    private static final DockerImageName MYSQL_IMAGE = DockerImageName.parse("mysql:8.4");
    private static final MySQLContainer<?> MYSQL_CONTAINER = new MySQLContainer<>(MYSQL_IMAGE);

    private static final String LIST_TABLES_QUERY =
        "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()";

    @BeforeAll
    static void startContainer() {
        MYSQL_CONTAINER.start(); // no-op when already running
    }

    @BeforeEach
    void cleanDatabase() throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                MYSQL_CONTAINER.getJdbcUrl(), MYSQL_CONTAINER.getUsername(), MYSQL_CONTAINER.getPassword());
             Statement statement = connection.createStatement()) {

            List<String> tables = new ArrayList<>();
            try (ResultSet resultSet = statement.executeQuery(LIST_TABLES_QUERY)) {
                while (resultSet.next()) {
                    tables.add(resultSet.getString(1));
                }
            }

            statement.execute("SET FOREIGN_KEY_CHECKS = 0");
            for (String table : tables) {
                statement.execute("DROP TABLE `" + table + "`");
            }
            statement.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }

    PluginConfig mysqlConfig() {
        PluginConfig config = new PluginConfig();
        config.settings.databaseType = DatabaseType.MYSQL;
        config.settings.host = MYSQL_CONTAINER.getHost();
        config.settings.port = String.valueOf(MYSQL_CONTAINER.getFirstMappedPort());
        config.settings.databaseName = MYSQL_CONTAINER.getDatabaseName();
        config.settings.user = MYSQL_CONTAINER.getUsername();
        config.settings.password = MYSQL_CONTAINER.getPassword();
        return config;
    }
}
