package com.countepiphany.dao;

import com.countepiphany.model.Supplier;
import com.countepiphany.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SupplierDAO — Akses data untuk entitas Supplier.
 */
public class SupplierDAO {

    private static final Logger LOGGER = Logger.getLogger(SupplierDAO.class.getName());

    public boolean save(Supplier supplier) {
        String sql = "INSERT INTO supplier (id_supplier, nama_supplier, alamat, telepon, email) "
                   + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, supplier.getIdSupplier());
            ps.setString(2, supplier.getNamaSupplier());
            ps.setString(3, supplier.getAlamat());
            ps.setString(4, supplier.getTelepon());
            ps.setString(5, supplier.getEmail());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal menyimpan supplier", e);
        }
        return false;
    }

    public boolean update(Supplier supplier) {
        String sql = "UPDATE supplier SET nama_supplier=?, alamat=?, telepon=?, email=? "
                   + "WHERE id_supplier=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, supplier.getNamaSupplier());
            ps.setString(2, supplier.getAlamat());
            ps.setString(3, supplier.getTelepon());
            ps.setString(4, supplier.getEmail());
            ps.setString(5, supplier.getIdSupplier());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal mengupdate supplier", e);
        }
        return false;
    }

    public boolean delete(String idSupplier) {
        String sql = "DELETE FROM supplier WHERE id_supplier=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, idSupplier);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal menghapus supplier", e);
        }
        return false;
    }

    public Optional<Supplier> findById(String idSupplier) {
        String sql = "SELECT * FROM supplier WHERE id_supplier=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, idSupplier);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal mencari supplier by id", e);
        }
        return Optional.empty();
    }

    public List<Supplier> findAll() {
        List<Supplier> list = new ArrayList<>();
        String sql = "SELECT * FROM supplier ORDER BY nama_supplier";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement  st   = conn.createStatement();
             ResultSet  rs   = st.executeQuery(sql)) {

            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal mengambil semua supplier", e);
        }
        return list;
    }

    /**
     * Mencari supplier berdasarkan nama (LIKE search).
     */
    public List<Supplier> findByNama(String keyword) {
        List<Supplier> list = new ArrayList<>();
        String sql = "SELECT * FROM supplier WHERE nama_supplier LIKE ? ORDER BY nama_supplier";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal mencari supplier by nama", e);
        }
        return list;
    }

    /**
     * Memeriksa apakah ID supplier sudah digunakan.
     */
    public boolean isIdExists(String idSupplier) {
        String sql = "SELECT 1 FROM supplier WHERE id_supplier=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, idSupplier);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Gagal cek id supplier", e);
        }
        return false;
    }

    private Supplier mapRow(ResultSet rs) throws SQLException {
        return new Supplier(
                rs.getString("id_supplier"),
                rs.getString("nama_supplier"),
                rs.getString("alamat"),
                rs.getString("telepon"),
                rs.getString("email")
        );
    }
}
