package com.eternalcode.parcellockers.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JdbcConnectionProvider {

    private final String dbUrl;
    private final String user;
    private final String pass;

    public JdbcConnectionProvider(String dbUrl, String user, String pass) {
        this.dbUrl = dbUrl;
        this.user = user;
        this.pass = pass;
    }

    public Connection createConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(this.dbUrl, this.user, this.pass);
        }
        catch (SQLException | ClassNotFoundException exception) {
            throw new RuntimeException(exception);
        }
    }

    public boolean executeUpdate(String sql) {
        try (Connection connection = this.createConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {
            return statement.execute();
        }
        catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    public ResultSet executeQuery(String sql) {
        try (Connection connection = this.createConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            return statement.executeQuery();
        }
        catch (SQLException exception) {
            exception.printStackTrace();
        }
        return null;
    }
}
