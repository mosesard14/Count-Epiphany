package com.countepiphany.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Model: LaporanKeuangan — rangkuman pendapatan dalam satu periode.
 */
public class LaporanKeuangan {

    private int           idLaporan;
    private LocalDate     tanggalMulai;
    private LocalDate     tanggalSelesai;
    private double        totalPendapatan;
    private LocalDateTime dibuatPada;
    private int           idKasir;

    // Field join
    private String namaKasir;

    // ── Konstruktor ──────────────────────────────────────────

    public LaporanKeuangan() {}

    public LaporanKeuangan(LocalDate tanggalMulai, LocalDate tanggalSelesai,
                           double totalPendapatan, int idKasir) {
        this.tanggalMulai    = tanggalMulai;
        this.tanggalSelesai  = tanggalSelesai;
        this.totalPendapatan = totalPendapatan;
        this.idKasir         = idKasir;
    }

    // ── Getter & Setter ───────────────────────────────────────

    public int  getIdLaporan()               { return idLaporan; }
    public void setIdLaporan(int idLaporan)  { this.idLaporan = idLaporan; }

    public LocalDate getTanggalMulai()                   { return tanggalMulai; }
    public void      setTanggalMulai(LocalDate tanggal)  { this.tanggalMulai = tanggal; }

    public LocalDate getTanggalSelesai()                   { return tanggalSelesai; }
    public void      setTanggalSelesai(LocalDate tanggal)  { this.tanggalSelesai = tanggal; }

    public double getTotalPendapatan()               { return totalPendapatan; }
    public void   setTotalPendapatan(double total)   { this.totalPendapatan = total; }

    public LocalDateTime getDibuatPada()                   { return dibuatPada; }
    public void          setDibuatPada(LocalDateTime dt)   { this.dibuatPada = dt; }

    public int  getIdKasir()            { return idKasir; }
    public void setIdKasir(int idKasir) { this.idKasir = idKasir; }

    public String getNamaKasir()                 { return namaKasir; }
    public void   setNamaKasir(String namaKasir) { this.namaKasir = namaKasir; }
}
