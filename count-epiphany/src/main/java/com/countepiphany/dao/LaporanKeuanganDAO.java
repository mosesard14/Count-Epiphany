package com.countepiphany.dao;

import com.countepiphany.model.LaporanKeuangan;
import com.countepiphany.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * LaporanKeuanganDAO — Akses data untuk entitas LaporanKeuangan.
 */
public class LaporanKeuanganDAO {

    private static final Logger LOGGER = Logger.getLogger(LaporanKeuanganDAO.class.getName());

    public boolean save(LaporanKeuangan laporan) {
        String sql = "INSERT INTO laporan_keuangan "
                   + "(tanggal_mulai, tanggal_selesai, total_pendapatan, id_kasir) "
                   + "VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setDate(1, Date.valueOf(laporan.getTanggalMulai()));
            ps.setDate(2, Date.valueOf(laporan.getTanggalSelesai()));
            ps.setDouble(3, laporan.getTotalPendapatan());
            ps.setInt   (4, laporan.getIdKasir());

            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) laporan.setIdLaporan(rs.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal menyimpan laporan keuangan", e);
        }
        return false;
    }

    public List<LaporanKeuangan> findAll() {
        List<LaporanKeuangan> list = new ArrayList<>();
        String sql = """
            SELECT lk.*, k.nama_kasir
            FROM laporan_keuangan lk
            LEFT JOIN kasir k ON lk.id_kasir = k.id_kasir
            ORDER BY lk.dibuat_pada DESC
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             Statement  st   = conn.createStatement();
             ResultSet  rs   = st.executeQuery(sql)) {

            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal mengambil semua laporan", e);
        }
        return list;
    }

    public List<LaporanKeuangan> findByPeriode(LocalDate dari, LocalDate sampai) {
        List<LaporanKeuangan> list = new ArrayList<>();
        String sql = """
            SELECT lk.*, k.nama_kasir
            FROM laporan_keuangan lk
            LEFT JOIN kasir k ON lk.id_kasir = k.id_kasir
            WHERE lk.tanggal_mulai >= ? AND lk.tanggal_selesai <= ?
            ORDER BY lk.dibuat_pada DESC
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(dari));
            ps.setDate(2, Date.valueOf(sampai));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal mengambil laporan by periode", e);
        }
        return list;
    }

    private LaporanKeuangan mapRow(ResultSet rs) throws SQLException {
        LaporanKeuangan l = new LaporanKeuangan();
        l.setIdLaporan(rs.getInt("id_laporan"));
        l.setTanggalMulai(rs.getDate("tanggal_mulai").toLocalDate());
        l.setTanggalSelesai(rs.getDate("tanggal_selesai").toLocalDate());
        l.setTotalPendapatan(rs.getDouble("total_pendapatan"));
        Timestamp ts = rs.getTimestamp("dibuat_pada");
        if (ts != null) l.setDibuatPada(ts.toLocalDateTime());
        l.setIdKasir(rs.getInt("id_kasir"));
        l.setNamaKasir(rs.getString("nama_kasir"));
        return l;
    }
}
