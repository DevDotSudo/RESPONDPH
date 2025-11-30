package com.ionres.respondph.database;

import java.io.FileInputStream;
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

    public static DBConnection getInstance() {
        if (instance == null) {
            synchronized (DBConnection.class) {
                if (instance == null) {
                    instance = new DBConnection();
                }
            }
        }
        return instance;
    }

    private Connection createConnection() {
        try {
            Properties props = new Properties();
            props.load(new FileInputStream("config/Outlet.properties"));
            if (props == null) {
                LOGGER.severe("Database configuration file not found.");
                throw new IOException("Database configuration file not found.");
            }
            String driver = props.getProperty("driver");
            String url = props.getProperty("url");
            String user = props.getProperty("user");
            String pass = props.getProperty("pass");

            Class.forName(driver);
            return DriverManager.getConnection(url, user, pass);
        } catch (IOException | ClassNotFoundException | SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to create database connection", ex);
            throw new RuntimeException("Database connection failure", ex);
        }
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = createConnection();
        }
        return connection;
    }
}