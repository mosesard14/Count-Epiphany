package com.countepiphany.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DatabaseConnection — Singleton utility untuk mengelola koneksi MySQL.
 * Membaca konfigurasi dari file database.properties di resources.
 */
public final class DatabaseConnection {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());
    private static final String PROPERTIES_FILE = "/database.properties";

    private static String url;
    private static String username;
    private static String password;

    static {
        loadProperties();
    }

    private DatabaseConnection() {
        // Utility class — tidak boleh diinstansiasi
    }

    /** Membaca konfigurasi koneksi dari database.properties */
    private static void loadProperties() {
        try (InputStream input = DatabaseConnection.class.getResourceAsStream(PROPERTIES_FILE)) {
            if (input == null) {
                throw new RuntimeException("File database.properties tidak ditemukan di resources.");
            }
            Properties props = new Properties();
            props.load(input);
            url      = props.getProperty("db.url");
            username = props.getProperty("db.username");
            password = props.getProperty("db.password");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Gagal membaca database.properties", e);
            throw new RuntimeException("Konfigurasi database tidak dapat dimuat.", e);
        }
    }

    /**
     * Mendapatkan koneksi baru ke database.
     * Pemanggil bertanggung jawab untuk menutup koneksi (gunakan try-with-resources).
     *
     * @return Connection aktif ke MySQL
     * @throws SQLException jika koneksi gagal
     */
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver tidak ditemukan.", e);
        }
        return DriverManager.getConnection(url, username, password);
    }

    /**
     * Menutup koneksi dengan aman (null-safe).
     *
     * @param connection koneksi yang akan ditutup
     */
    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Gagal menutup koneksi database", e);
            }
        }
    }

    /**
     * Menguji apakah koneksi ke database dapat dibuat.
     *
     * @return true jika koneksi berhasil
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Uji koneksi gagal: " + e.getMessage());
            return false;
        }
    }
}
