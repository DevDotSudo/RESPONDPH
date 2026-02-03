package com.ionres.respondph.database;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DBConnection {

    private static DBConnection instance;
    private static final Logger LOGGER = Logger.getLogger(DBConnection.class.getName());

    // Cached configuration so we don't reload properties on every call
    private String url;
    private String user;
    private String pass;

    private DBConnection() {
        loadConfig();
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

    private void loadConfig() {
        try (InputStream input = DBConnection.class.getResourceAsStream("/config/Outlet.properties")) {
            if (input == null) {
                LOGGER.severe("Database configuration file not found.");
                throw new IOException("Database configuration file not found.");
            }

            Properties outlet = new Properties();
            outlet.load(input);
            String driver = outlet.getProperty("driver");
            this.url = outlet.getProperty("url");
            this.user = outlet.getProperty("user");
            this.pass = outlet.getProperty("pass");

            Class.forName(driver);
        } catch (IOException | ClassNotFoundException ex) {
            LOGGER.log(Level.SEVERE, "Failed to load database configuration", ex);
            throw new RuntimeException("Database configuration failure", ex);
        }
    }

    /**
     * Returns a fresh JDBC connection on every call.
     * Callers are responsible for closing the connection (prefer try-with-resources).
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, pass);
    }
}
