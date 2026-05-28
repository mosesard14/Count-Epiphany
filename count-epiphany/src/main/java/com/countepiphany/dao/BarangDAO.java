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
 * Revisi: tambah subkategori untuk hierarki dua level
 *         kategori (induk) -> subkategori (jenis).
 */
public class BarangDAO {

    private static final Logger LOGGER = Logger.getLogger(BarangDAO.class.getName());

    public boolean save(Barang barang) {
        String sql = "INSERT INTO barang (id_barang, nama_barang, harga_beli, harga_jual, "
                   + "stok, kategori, subkategori, id_supplier, stok_minimum) "
                   + "VALUES (?,?,?,?,?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, barang.getIdBarang());
            ps.setString(2, barang.getNamaBarang());
            ps.setDouble(3, barang.getHargaBeli());
            ps.setDouble(4, barang.getHargaJual());
            ps.setInt   (5, barang.getStok());
            ps.setString(6, barang.getKategori());
            ps.setString(7, barang.getSubkategori());
            ps.setString(8, barang.getIdSupplier());
            ps.setInt   (9, barang.getStokMinimum());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal menyimpan barang", e);
        }
        return false;
    }

    public boolean update(Barang barang) {
        String sql = "UPDATE barang SET nama_barang=?, harga_beli=?, harga_jual=?, "
                   + "kategori=?, subkategori=?, id_supplier=?, stok_minimum=? "
                   + "WHERE id_barang=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, barang.getNamaBarang());
            ps.setDouble(2, barang.getHargaBeli());
            ps.setDouble(3, barang.getHargaJual());
            ps.setString(4, barang.getKategori());
            ps.setString(5, barang.getSubkategori());
            ps.setString(6, barang.getIdSupplier());
            ps.setInt   (7, barang.getStokMinimum());
            ps.setString(8, barang.getIdBarang());

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
        String sql = "SELECT * FROM barang ORDER BY kategori, subkategori, nama_barang";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement  st   = conn.createStatement();
             ResultSet  rs   = st.executeQuery(sql)) {

            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal mengambil semua barang", e);
        }
        return list;
    }

    public List<Barang> findByKeyword(String keyword) {
        List<Barang> list = new ArrayList<>();
        String sql = "SELECT * FROM barang "
                   + "WHERE nama_barang LIKE ? OR id_barang LIKE ? "
                   + "OR subkategori LIKE ? OR kategori LIKE ? "
                   + "ORDER BY kategori, subkategori, nama_barang";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String kw = "%" + keyword + "%";
            ps.setString(1, kw);
            ps.setString(2, kw);
            ps.setString(3, kw);
            ps.setString(4, kw);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal mencari barang by keyword", e);
        }
        return list;
    }

    public List<Barang> findByKategori(String kategori) {
        List<Barang> list = new ArrayList<>();
        String sql = "SELECT * FROM barang WHERE kategori=? "
                   + "ORDER BY subkategori, nama_barang";
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
     * Filter by kategori DAN subkategori sekaligus.
     * Dipakai saat user klik tab kategori lalu pilih subkategori dari panel kiri.
     */
    public List<Barang> findByKategoriDanSubkategori(String kategori, String subkategori) {
        List<Barang> list = new ArrayList<>();
        String sql = "SELECT * FROM barang WHERE kategori=? AND subkategori=? "
                   + "ORDER BY nama_barang";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, kategori);
            ps.setString(2, subkategori);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal mencari barang by kategori+subkategori", e);
        }
        return list;
    }

    /**
     * Ambil semua subkategori dari satu kategori tertentu.
     * Dipakai untuk mengisi panel kiri daftar jenis/subkategori.
     */
    public List<String> findSubkategoriByKategori(String kategori) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT DISTINCT subkategori FROM barang "
                   + "WHERE kategori=? AND subkategori IS NOT NULL "
                   + "ORDER BY subkategori";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, kategori);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getString("subkategori"));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal mengambil subkategori", e);
        }
        return list;
    }

    public List<String> findAllKategori() {
        List<String> list = new ArrayList<>();
        String sql = "SELECT DISTINCT kategori FROM barang "
                   + "WHERE kategori IS NOT NULL ORDER BY kategori";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement  st   = conn.createStatement();
             ResultSet  rs   = st.executeQuery(sql)) {

            while (rs.next()) list.add(rs.getString("kategori"));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal mengambil kategori", e);
        }
        return list;
    }

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
        Barang b = new Barang(
                rs.getString("id_barang"),
                rs.getString("nama_barang"),
                rs.getDouble("harga_beli"),
                rs.getDouble("harga_jual"),
                rs.getInt   ("stok"),
                rs.getString("kategori"),
                rs.getString("id_supplier"),
                rs.getInt   ("stok_minimum")
        );
        b.setSubkategori(rs.getString("subkategori"));
        return b;
    }
}
