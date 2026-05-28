package com.countepiphany.util;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DatabaseInitializer — Inisialisasi data awal saat aplikasi pertama kali dijalankan.
 *
 * Cara kerja:
 *   1. Cek apakah user 'admin' sudah ada di tabel kasir.
 *   2. Kalau belum ada, insert otomatis dengan hash BCrypt yang di-generate fresh.
 *   3. Dipanggil sekali saat startup dari MainApp.
 *
 * Keuntungan:
 *   - Tidak perlu hardcode hash di schema.sql.
 *   - Setiap developer tinggal jalankan app, admin langsung tersedia.
 *   - Hash selalu valid karena di-generate saat runtime.
 */
public final class DatabaseInitializer {

    private static final Logger LOGGER = Logger.getLogger(DatabaseInitializer.class.getName());

    // Kredensial default admin — ubah sesuai kebutuhan project
    private static final String DEFAULT_ADMIN_NAMA     = "Administrator";
    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin123";

    private DatabaseInitializer() {}

    /**
     * Entry point — panggil ini dari MainApp.start() sebelum tampilkan halaman login.
     */
    public static void initialize() {
        LOGGER.info("DatabaseInitializer: mulai pengecekan data awal...");

        if (!adminExists()) {
            createDefaultAdmin();
        } else {
            LOGGER.info("DatabaseInitializer: user admin sudah ada, skip.");
        }
    }

    // ── Private helpers ──────────────────────────────────────

    /**
     * Cek apakah username 'admin' sudah ada di database.
     */
    private static boolean adminExists() {
        String sql = "SELECT COUNT(*) FROM kasir WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, DEFAULT_ADMIN_USERNAME);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Gagal cek keberadaan admin: " + e.getMessage());
        }
        return false;
    }

    /**
     * Insert admin baru dengan password yang di-hash BCrypt saat runtime.
     * Hash di-generate fresh — tidak pernah hardcode di kode maupun SQL.
     */
    private static void createDefaultAdmin() {
        // Hash di-generate di sini, bukan dari nilai statis
        String hashedPassword = BCrypt.hashpw(DEFAULT_ADMIN_PASSWORD, BCrypt.gensalt(12));

        String sql = "INSERT INTO kasir (nama_kasir, username, password) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, DEFAULT_ADMIN_NAMA);
            ps.setString(2, DEFAULT_ADMIN_USERNAME);
            ps.setString(3, hashedPassword);

            ps.executeUpdate();
            LOGGER.info("DatabaseInitializer: admin default berhasil dibuat.");
            LOGGER.info("  Username : " + DEFAULT_ADMIN_USERNAME);
            LOGGER.info("  Password : " + DEFAULT_ADMIN_PASSWORD);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal membuat admin default: " + e.getMessage());
        }
    }
}
