package com.countepiphany.dao;

import com.countepiphany.model.PembelianBarang;
import com.countepiphany.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PembelianBarangDAO — Akses data untuk entitas PembelianBarang.
 * Setiap INSERT akan otomatis menambah stok melalui trigger database.
 */
public class PembelianBarangDAO {

    private static final Logger LOGGER = Logger.getLogger(PembelianBarangDAO.class.getName());

    /**
     * Menyimpan satu transaksi pembelian barang.
     * Trigger database akan otomatis menambah stok barang yang bersangkutan.
     */
    public boolean save(PembelianBarang pembelian) {
        String sql = "INSERT INTO pembelian_barang "
                   + "(tanggal, id_supplier, id_barang, jumlah_beli, harga_beli_satuan, id_kasir) "
                   + "VALUES (NOW(), ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, pembelian.getIdSupplier());
            ps.setString(2, pembelian.getIdBarang());
            ps.setInt   (3, pembelian.getJumlahBeli());
            ps.setDouble(4, pembelian.getHargaBeliSatuan());
            ps.setInt   (5, pembelian.getIdKasir());

            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) pembelian.setIdPembelian(rs.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal menyimpan pembelian barang", e);
        }
        return false;
    }

    /**
     * Mengambil semua riwayat pembelian dengan JOIN ke tabel terkait.
     */
    public List<PembelianBarang> findAll() {
        List<PembelianBarang> list = new ArrayList<>();
        String sql = """
            SELECT pb.*,
                   s.nama_supplier,
                   b.nama_barang,
                   k.nama_kasir
            FROM pembelian_barang pb
            LEFT JOIN supplier s ON pb.id_supplier = s.id_supplier
            LEFT JOIN barang   b ON pb.id_barang   = b.id_barang
            LEFT JOIN kasir    k ON pb.id_kasir     = k.id_kasir
            ORDER BY pb.tanggal DESC
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             Statement  st   = conn.createStatement();
             ResultSet  rs   = st.executeQuery(sql)) {

            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal mengambil semua pembelian", e);
        }
        return list;
    }

    /**
     * Mengambil riwayat pembelian berdasarkan rentang tanggal.
     */
    public List<PembelianBarang> findByPeriode(java.time.LocalDate dari,
                                                java.time.LocalDate sampai) {
        List<PembelianBarang> list = new ArrayList<>();
        String sql = """
            SELECT pb.*,
                   s.nama_supplier,
                   b.nama_barang,
                   k.nama_kasir
            FROM pembelian_barang pb
            LEFT JOIN supplier s ON pb.id_supplier = s.id_supplier
            LEFT JOIN barang   b ON pb.id_barang   = b.id_barang
            LEFT JOIN kasir    k ON pb.id_kasir     = k.id_kasir
            WHERE DATE(pb.tanggal) BETWEEN ? AND ?
            ORDER BY pb.tanggal DESC
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(dari));
            ps.setDate(2, Date.valueOf(sampai));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal mengambil pembelian by periode", e);
        }
        return list;
    }

    /**
     * Menghitung total modal pembelian dalam satu periode.
     */
    public double sumTotalModalByPeriode(java.time.LocalDate dari,
                                          java.time.LocalDate sampai) {
        String sql = "SELECT COALESCE(SUM(total_modal), 0) FROM pembelian_barang "
                   + "WHERE DATE(tanggal) BETWEEN ? AND ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(dari));
            ps.setDate(2, Date.valueOf(sampai));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal menghitung total modal", e);
        }
        return 0;
    }

    private PembelianBarang mapRow(ResultSet rs) throws SQLException {
        PembelianBarang p = new PembelianBarang();
        p.setIdPembelian(rs.getInt("id_pembelian"));
        p.setTanggal(rs.getTimestamp("tanggal").toLocalDateTime());
        p.setIdSupplier(rs.getString("id_supplier"));
        p.setIdBarang(rs.getString("id_barang"));
        p.setJumlahBeli(rs.getInt("jumlah_beli"));
        p.setHargaBeliSatuan(rs.getDouble("harga_beli_satuan"));
        p.setTotalModal(rs.getDouble("total_modal"));
        p.setIdKasir(rs.getInt("id_kasir"));
        // JOIN fields
        p.setNamaSupplier(rs.getString("nama_supplier"));
        p.setNamaBarang(rs.getString("nama_barang"));
        p.setNamaKasir(rs.getString("nama_kasir"));
        return p;
    }
}
