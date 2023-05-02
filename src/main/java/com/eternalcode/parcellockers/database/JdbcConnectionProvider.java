package com.eternalcode.parcellockers.database;

import com.eternalcode.parcellockers.exception.ParcelLockersException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JdbcConnectionProvider {

    private final String dbUrl;
    private final String user;
    private final String pass;
    private final String finalUrl;

    public JdbcConnectionProvider(String dbUrl, String port, String databaseName, boolean useSSL, String user, String pass) {
        this.dbUrl = dbUrl;
        this.user = user;
        this.pass = pass;
        this.finalUrl = "jdbc:mysql://" + dbUrl + ":" + port + "/" + databaseName + "?useSSL=" + useSSL;
    }

    public Connection createConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(this.finalUrl, this.user, this.pass);
        }
        catch (SQLException | ClassNotFoundException exception) {
            throw new ParcelLockersException(exception);
        }
    }

    public boolean executeUpdate(String sql) {
        try (Connection connection = this.createConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {
            return statement.execute();
        }
        catch (SQLException exception) {
            throw new ParcelLockersException(exception);
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
