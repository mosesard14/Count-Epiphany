package com.countepiphany.dao;

import com.countepiphany.model.DetailTransaksi;
import com.countepiphany.model.Transaksi;
import com.countepiphany.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.time.LocalDateTime;


/**
 * TransaksiDAO — Akses data untuk entitas Transaksi dan DetailTransaksi.
 * Menyimpan header transaksi dan semua item-nya dalam satu transaksi database (atomic).
 */
public class TransaksiDAO {

    private static final Logger LOGGER = Logger.getLogger(TransaksiDAO.class.getName());

    /**
     * Menyimpan transaksi lengkap (header + semua detail item) secara atomis.
     * Trigger database akan otomatis mengurangi stok untuk setiap detail yang di-insert.
     *
     * @return true jika semua berhasil disimpan
     */
    public boolean saveTransaksiFull(Transaksi transaksi) {
        String sqlHeader = "INSERT INTO transaksi "
                + "(tanggal, total_harga, diskon, jumlah_bayar, kembalian, metode_bayar, id_kasir) "
                + "VALUES (NOW(), ?, ?, ?, ?, ?, ?)";
        String sqlDetail = "INSERT INTO detail_transaksi "
                + "(id_transaksi, id_barang, jumlah, harga_satuan, diskon_item, subtotal) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);  // mulai transaksi database

            // 1. Simpan header
            int idTransaksi;
            try (PreparedStatement ps = conn.prepareStatement(sqlHeader,
                    Statement.RETURN_GENERATED_KEYS)) {

                ps.setDouble(1, transaksi.getTotalHarga());
                ps.setDouble(2, transaksi.getDiskon());
                ps.setDouble(3, transaksi.getJumlahBayar());
                ps.setDouble(4, transaksi.getKembalian());
                ps.setString(5, transaksi.getMetodeBayar().name());
                ps.setInt   (6, transaksi.getIdKasir());
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (!rs.next()) throw new SQLException("Gagal mendapatkan ID transaksi.");
                    idTransaksi = rs.getInt(1);
                    transaksi.setIdTransaksi(idTransaksi);
                    transaksi.setTanggal(LocalDateTime.now());
                }
            }

            // 2. Simpan semua detail item
            try (PreparedStatement ps = conn.prepareStatement(sqlDetail)) {
                for (DetailTransaksi det : transaksi.getDetailList()) {
                    ps.setInt   (1, idTransaksi);
                    ps.setString(2, det.getIdBarang());
                    ps.setInt   (3, det.getJumlah());
                    ps.setDouble(4, det.getHargaSatuan());
                    ps.setDouble(5, det.getDiskonItem());
                    ps.setDouble(6, det.getSubtotal());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal menyimpan transaksi — rollback", e);
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Rollback gagal", ex);
                }
            }
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
        return false;
    }

    /**
     * Mengambil transaksi berdasarkan ID beserta detail item-nya.
     */
    public Optional<Transaksi> findById(int idTransaksi) {
        String sql = """
            SELECT t.*, k.nama_kasir
            FROM transaksi t
            LEFT JOIN kasir k ON t.id_kasir = k.id_kasir
            WHERE t.id_transaksi = ?
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idTransaksi);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Transaksi t = mapHeaderRow(rs);
                    t.setDetailList(findDetailByTransaksiId(idTransaksi));
                    return Optional.of(t);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal mencari transaksi by id", e);
        }
        return Optional.empty();
    }

    /**
     * Mengambil semua header transaksi dengan JOIN ke kasir.
     */
    public List<Transaksi> findAll() {
        List<Transaksi> list = new ArrayList<>();
        String sql = """
            SELECT t.*, k.nama_kasir
            FROM transaksi t
            LEFT JOIN kasir k ON t.id_kasir = k.id_kasir
            ORDER BY t.tanggal DESC
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             Statement  st   = conn.createStatement();
             ResultSet  rs   = st.executeQuery(sql)) {

            while (rs.next()) list.add(mapHeaderRow(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal mengambil semua transaksi", e);
        }
        return list;
    }

    /**
     * Mengambil transaksi berdasarkan rentang tanggal.
     */
    public List<Transaksi> findByPeriode(LocalDate dari, LocalDate sampai) {
        List<Transaksi> list = new ArrayList<>();
        String sql = """
            SELECT t.*, k.nama_kasir
            FROM transaksi t
            LEFT JOIN kasir k ON t.id_kasir = k.id_kasir
            WHERE DATE(t.tanggal) BETWEEN ? AND ?
            ORDER BY t.tanggal DESC
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(dari));
            ps.setDate(2, Date.valueOf(sampai));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapHeaderRow(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal mencari transaksi by periode", e);
        }
        return list;
    }

    /**
     * Menghitung total pendapatan (sum total_harga) dalam satu periode.
     */
    public double sumPendapatanByPeriode(LocalDate dari, LocalDate sampai) {
        String sql = "SELECT COALESCE(SUM(total_harga), 0) FROM transaksi "
                + "WHERE DATE(tanggal) BETWEEN ? AND ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(dari));
            ps.setDate(2, Date.valueOf(sampai));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal menghitung pendapatan", e);
        }
        return 0;
    }

    /**
     * Mengambil detail item untuk satu transaksi, termasuk nama barang.
     */
    public List<DetailTransaksi> findDetailByTransaksiId(int idTransaksi) {
        List<DetailTransaksi> list = new ArrayList<>();
        String sql = """
            SELECT dt.*, b.nama_barang
            FROM detail_transaksi dt
            LEFT JOIN barang b ON dt.id_barang = b.id_barang
            WHERE dt.id_transaksi = ?
            ORDER BY dt.id_detail
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idTransaksi);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapDetailRow(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal mengambil detail transaksi", e);
        }
        return list;
    }

    /**
     * Mengambil laporan per barang untuk halaman Laporan Keuangan.
     * Mengembalikan: kode, kategori, nama barang, sisa stok, terjual, harga jual, nominal penjualan.
     */
    public List<Object[]> getLaporanBarang(LocalDate dari, LocalDate sampai) {
        List<Object[]> list = new ArrayList<>();
        String sql = """
            SELECT b.id_barang, b.kategori, b.nama_barang, b.stok,
                   COALESCE(SUM(dt.jumlah), 0)    AS terjual,
                   b.harga_jual,
                   COALESCE(SUM(dt.subtotal), 0)  AS nominal_penjualan
            FROM barang b
            LEFT JOIN detail_transaksi dt ON b.id_barang = dt.id_barang
            LEFT JOIN transaksi t ON dt.id_transaksi = t.id_transaksi
                AND DATE(t.tanggal) BETWEEN ? AND ?
            GROUP BY b.id_barang, b.kategori, b.nama_barang, b.stok, b.harga_jual
            ORDER BY nominal_penjualan DESC
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(dari));
            ps.setDate(2, Date.valueOf(sampai));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Object[]{
                            rs.getString("id_barang"),
                            rs.getString("kategori"),
                            rs.getString("nama_barang"),
                            rs.getInt("stok"),
                            rs.getInt("terjual"),
                            rs.getDouble("harga_jual"),
                            rs.getDouble("nominal_penjualan")
                    });
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal mengambil laporan barang", e);
        }
        return list;
    }

    // ── Mapping ──────────────────────────────────────────────

    private Transaksi mapHeaderRow(ResultSet rs) throws SQLException {
        Transaksi t = new Transaksi();
        t.setIdTransaksi(rs.getInt("id_transaksi"));
        t.setTanggal(rs.getTimestamp("tanggal").toLocalDateTime());
        t.setTotalHarga(rs.getDouble("total_harga"));
        t.setDiskon(rs.getDouble("diskon"));
        t.setJumlahBayar(rs.getDouble("jumlah_bayar"));
        t.setKembalian(rs.getDouble("kembalian"));
        t.setMetodeBayar(Transaksi.MetodeBayar.valueOf(rs.getString("metode_bayar")));
        t.setIdKasir(rs.getInt("id_kasir"));
        t.setNamaKasir(rs.getString("nama_kasir"));
        return t;
    }

    private DetailTransaksi mapDetailRow(ResultSet rs) throws SQLException {
        DetailTransaksi d = new DetailTransaksi();
        d.setIdDetail(rs.getInt("id_detail"));
        d.setIdTransaksi(rs.getInt("id_transaksi"));
        d.setIdBarang(rs.getString("id_barang"));
        d.setKodeBarang(rs.getString("id_barang"));
        d.setNamaBarang(rs.getString("nama_barang"));
        d.setJumlah(rs.getInt("jumlah"));
        d.setHargaSatuan(rs.getDouble("harga_satuan"));
        d.setDiskonItem(rs.getDouble("diskon_item"));
        d.setSubtotal(rs.getDouble("subtotal"));
        return d;
    }
}
