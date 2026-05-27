package com.countepiphany.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Model: Transaksi — header setiap penjualan di kasir.
 * Menyimpan informasi ringkasan transaksi; detail item ada di DetailTransaksi.
 */
public class Transaksi {

    /** Enum metode pembayaran sesuai kolom ENUM di database. */
    public enum MetodeBayar {
        cash, cashless
    }

    private int           idTransaksi;
    private LocalDateTime tanggal;
    private double        totalHarga;
    private double        diskon;        // persentase (0-100)
    private double        jumlahBayar;
    private double        kembalian;
    private MetodeBayar   metodeBayar;
    private int           idKasir;

    // Field join untuk tampilan
    private String namaKasir;

    // Daftar item transaksi (transient — tidak langsung ke satu tabel)
    private List<DetailTransaksi> detailList = new ArrayList<>();

    // ── Konstruktor ──────────────────────────────────────────

    public Transaksi() {
        this.metodeBayar = MetodeBayar.cash;
        this.diskon      = 0;
    }

    // ── Business Logic ───────────────────────────────────────

    /**
     * Menghitung total harga dari semua item di detailList,
     * lalu menerapkan diskon jika ada.
     */
    public double hitungTotal() {
        double subtotal = detailList.stream()
                .mapToDouble(DetailTransaksi::getSubtotal)
                .sum();
        totalHarga = subtotal * (1 - diskon / 100.0);
        return totalHarga;
    }

    /**
     * Menghitung kembalian: jumlahBayar - totalHarga.
     * Mengembalikan 0 jika cashless (tidak ada uang tunai).
     */
    public double hitungKembalian() {
        if (metodeBayar == MetodeBayar.cashless) {
            kembalian = 0;
        } else {
            kembalian = jumlahBayar - totalHarga;
        }
        return kembalian;
    }

    // ── Getter & Setter ───────────────────────────────────────

    public int  getIdTransaksi()                  { return idTransaksi; }
    public void setIdTransaksi(int idTransaksi)   { this.idTransaksi = idTransaksi; }

    public LocalDateTime getTanggal()                 { return tanggal; }
    public void          setTanggal(LocalDateTime t)  { this.tanggal = t; }

    public double getTotalHarga()              { return totalHarga; }
    public void   setTotalHarga(double total)  { this.totalHarga = total; }

    public double getDiskon()              { return diskon; }
    public void   setDiskon(double diskon) { this.diskon = diskon; }

    public double getJumlahBayar()                { return jumlahBayar; }
    public void   setJumlahBayar(double jumlahBayar) { this.jumlahBayar = jumlahBayar; }

    public double getKembalian()               { return kembalian; }
    public void   setKembalian(double kembalian) { this.kembalian = kembalian; }

    public MetodeBayar getMetodeBayar()                    { return metodeBayar; }
    public void        setMetodeBayar(MetodeBayar metode)  { this.metodeBayar = metode; }

    public int  getIdKasir()            { return idKasir; }
    public void setIdKasir(int idKasir) { this.idKasir = idKasir; }

    public String getNamaKasir()                 { return namaKasir; }
    public void   setNamaKasir(String namaKasir) { this.namaKasir = namaKasir; }

    public List<DetailTransaksi> getDetailList()                         { return detailList; }
    public void                  setDetailList(List<DetailTransaksi> dl) { this.detailList = dl; }

    public void tambahItem(DetailTransaksi item) {
        detailList.add(item);
    }
}
