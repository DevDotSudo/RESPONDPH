package com.ionres.respondph.database;

import java.io.InputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DBConnection {

    private static DBConnection instance;
    private Connection connection;
    private static final Logger LOGGER = Logger.getLogger(DBConnection.class.getName());

    private DBConnection() {
        connection = createConnection();
    }

    public static synchronized DBConnection getInstance() {
        if (instance == null) {
            instance = new DBConnection();
        }
        return instance;
    }

    private Connection createConnection() {
        try (InputStream input = DBConnection.class.getResourceAsStream("/config/Outlet.properties")) {
            if (input == null) {
                LOGGER.severe("Database configuration file not found.");
                return null;
            }

            Properties outlet = new Properties();
            outlet.load(input);
            Class.forName(outlet.getProperty("driver"));

            return DriverManager.getConnection(
                    outlet.getProperty("url"),
                    outlet.getProperty("user"),
                    outlet.getProperty("pass")
            );
        } catch (IOException | ClassNotFoundException | SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to create database connection", ex);
            return null;
        }
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = createConnection();
            if (connection == null) {
                throw new SQLException("Failed to re-establish database connection.");
            }
        }
        return connection;
    }

    public static void main(String[] args) {
        try {
            System.out.println(DBConnection.getInstance().getConnection());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
