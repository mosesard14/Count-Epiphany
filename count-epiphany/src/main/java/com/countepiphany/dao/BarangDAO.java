package com.countepiphany.dao;

import com.countepiphany.model.Barang;
import com.countepiphany.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * BarangDAO — Akses data untuk entitas Barang.
 * Catatan: penambahan stok hanya boleh melalui PembelianBarangDAO (via trigger).
 *          DAO ini hanya mengizinkan update data barang (nama, harga, kategori, dll.)
 *          bukan langsung mengubah nilai stok.
 */
public class BarangDAO {

    private static final Logger LOGGER = Logger.getLogger(BarangDAO.class.getName());

    /**
     * Menyimpan barang baru. Stok awal = 0; harus di-restock via pembelian.
     */
    public boolean save(Barang barang) {
        String sql = "INSERT INTO barang (id_barang, nama_barang, harga_beli, harga_jual, "
                   + "stok, kategori, id_supplier, stok_minimum) VALUES (?,?,?,?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, barang.getIdBarang());
            ps.setString(2, barang.getNamaBarang());
            ps.setDouble(3, barang.getHargaBeli());
            ps.setDouble(4, barang.getHargaJual());
            ps.setInt   (5, barang.getStok());
            ps.setString(6, barang.getKategori());
            ps.setString(7, barang.getIdSupplier());
            ps.setInt   (8, barang.getStokMinimum());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal menyimpan barang", e);
        }
        return false;
    }

    /**
     * Mengupdate informasi barang (harga, nama, kategori, stok_minimum).
     * Tidak mengubah stok secara langsung.
     */
    public boolean update(Barang barang) {
        String sql = "UPDATE barang SET nama_barang=?, harga_beli=?, harga_jual=?, "
                   + "kategori=?, id_supplier=?, stok_minimum=? WHERE id_barang=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, barang.getNamaBarang());
            ps.setDouble(2, barang.getHargaBeli());
            ps.setDouble(3, barang.getHargaJual());
            ps.setString(4, barang.getKategori());
            ps.setString(5, barang.getIdSupplier());
            ps.setInt   (6, barang.getStokMinimum());
            ps.setString(7, barang.getIdBarang());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal mengupdate barang", e);
        }
        return false;
    }

    public boolean delete(String idBarang) {
        String sql = "DELETE FROM barang WHERE id_barang=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, idBarang);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal menghapus barang", e);
        }
        return false;
    }

    public Optional<Barang> findById(String idBarang) {
        String sql = "SELECT * FROM barang WHERE id_barang=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, idBarang);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal mencari barang by id", e);
        }
        return Optional.empty();
    }

    public List<Barang> findAll() {
        List<Barang> list = new ArrayList<>();
        String sql = "SELECT * FROM barang ORDER BY nama_barang";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement  st   = conn.createStatement();
             ResultSet  rs   = st.executeQuery(sql)) {

            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal mengambil semua barang", e);
        }
        return list;
    }

    /**
     * Mencari barang berdasarkan nama atau kode (LIKE search).
     */
    public List<Barang> findByKeyword(String keyword) {
        List<Barang> list = new ArrayList<>();
        String sql = "SELECT * FROM barang WHERE nama_barang LIKE ? OR id_barang LIKE ? "
                   + "ORDER BY nama_barang";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String kw = "%" + keyword + "%";
            ps.setString(1, kw);
            ps.setString(2, kw);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal mencari barang by keyword", e);
        }
        return list;
    }

    /**
     * Filter barang berdasarkan kategori.
     */
    public List<Barang> findByKategori(String kategori) {
        List<Barang> list = new ArrayList<>();
        String sql = "SELECT * FROM barang WHERE kategori=? ORDER BY nama_barang";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, kategori);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal mencari barang by kategori", e);
        }
        return list;
    }

    /**
     * Mendapatkan semua kategori yang terdaftar (untuk dropdown filter).
     */
    public List<String> findAllKategori() {
        List<String> list = new ArrayList<>();
        String sql = "SELECT DISTINCT kategori FROM barang WHERE kategori IS NOT NULL ORDER BY kategori";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement  st   = conn.createStatement();
             ResultSet  rs   = st.executeQuery(sql)) {

            while (rs.next()) list.add(rs.getString("kategori"));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal mengambil kategori", e);
        }
        return list;
    }

    /**
     * Mendapatkan daftar barang yang stoknya di bawah minimum.
     */
    public List<Barang> findStokRendah() {
        List<Barang> list = new ArrayList<>();
        String sql = "SELECT * FROM barang WHERE stok <= stok_minimum ORDER BY stok ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement  st   = conn.createStatement();
             ResultSet  rs   = st.executeQuery(sql)) {

            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal mengambil barang stok rendah", e);
        }
        return list;
    }

    /**
     * Memeriksa apakah ID barang sudah digunakan.
     */
    public boolean isIdExists(String idBarang) {
        String sql = "SELECT 1 FROM barang WHERE id_barang=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, idBarang);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Gagal cek id barang", e);
        }
        return false;
    }

    private Barang mapRow(ResultSet rs) throws SQLException {
        return new Barang(
                rs.getString("id_barang"),
                rs.getString("nama_barang"),
                rs.getDouble("harga_beli"),
                rs.getDouble("harga_jual"),
                rs.getInt("stok"),
                rs.getString("kategori"),
                rs.getString("id_supplier"),
                rs.getInt("stok_minimum")
        );
    }
}
