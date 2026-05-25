package com.countepiphany.dao;

import com.countepiphany.model.Kasir;
import com.countepiphany.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * KasirDAO — Akses data untuk entitas Kasir.
 * Semua operasi JDBC terisolasi di sini; tidak ada SQL di layer lain.
 */
public class KasirDAO {

    private static final Logger LOGGER = Logger.getLogger(KasirDAO.class.getName());

    // ── CRUD ─────────────────────────────────────────────────

    /**
     * Menyimpan kasir baru ke database.
     * @return true jika berhasil
     */
    public boolean save(Kasir kasir) {
        String sql = "INSERT INTO kasir (nama_kasir, username, password) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, kasir.getNamaKasir());
            ps.setString(2, kasir.getUsername());
            ps.setString(3, kasir.getPassword());

            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) kasir.setIdKasir(rs.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal menyimpan kasir", e);
        }
        return false;
    }

    /**
     * Mengupdate data kasir yang sudah ada.
     */
    public boolean update(Kasir kasir) {
        String sql = "UPDATE kasir SET nama_kasir=?, username=?, password=? WHERE id_kasir=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, kasir.getNamaKasir());
            ps.setString(2, kasir.getUsername());
            ps.setString(3, kasir.getPassword());
            ps.setInt(4, kasir.getIdKasir());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal mengupdate kasir", e);
        }
        return false;
    }

    /**
     * Menghapus kasir berdasarkan ID.
     */
    public boolean delete(int idKasir) {
        String sql = "DELETE FROM kasir WHERE id_kasir=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idKasir);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal menghapus kasir", e);
        }
        return false;
    }

    /**
     * Mencari kasir berdasarkan username.
     */
    public Optional<Kasir> findByUsername(String username) {
        String sql = "SELECT * FROM kasir WHERE username=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal mencari kasir by username", e);
        }
        return Optional.empty();
    }

    /**
     * Mencari kasir berdasarkan ID.
     */
    public Optional<Kasir> findById(int idKasir) {
        String sql = "SELECT * FROM kasir WHERE id_kasir=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idKasir);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal mencari kasir by id", e);
        }
        return Optional.empty();
    }

    /**
     * Mengambil semua data kasir.
     */
    public List<Kasir> findAll() {
        List<Kasir> list = new ArrayList<>();
        String sql = "SELECT * FROM kasir ORDER BY nama_kasir";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement  st   = conn.createStatement();
             ResultSet  rs   = st.executeQuery(sql)) {

            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal mengambil semua kasir", e);
        }
        return list;
    }

    // ── Mapping ──────────────────────────────────────────────

    private Kasir mapRow(ResultSet rs) throws SQLException {
        return new Kasir(
                rs.getInt("id_kasir"),
                rs.getString("nama_kasir"),
                rs.getString("username"),
                rs.getString("password")
        );
    }
}
