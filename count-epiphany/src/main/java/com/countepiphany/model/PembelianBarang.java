package com.countepiphany.model;

import java.time.LocalDateTime;

/**
 * Model: PembelianBarang — merepresentasikan satu transaksi restock dari supplier.
 * Setiap pembelian yang disimpan otomatis menambah stok barang melalui trigger database.
 */
public class PembelianBarang {

    private int           idPembelian;
    private LocalDateTime tanggal;
    private String        idSupplier;
    private String        idBarang;
    private int           jumlahBeli;
    private double        hargaBeliSatuan;
    private double        totalModal;        // generated di DB: jumlahBeli * hargaBeliSatuan
    private int           idKasir;

    // Field join (tidak tersimpan di tabel ini, hanya untuk tampilan)
    private String namaSupplier;
    private String namaBarang;
    private String namaKasir;

    // ── Konstruktor ──────────────────────────────────────────

    public PembelianBarang() {}

    public PembelianBarang(String idSupplier, String idBarang,
                           int jumlahBeli, double hargaBeliSatuan, int idKasir) {
        this.idSupplier      = idSupplier;
        this.idBarang        = idBarang;
        this.jumlahBeli      = jumlahBeli;
        this.hargaBeliSatuan = hargaBeliSatuan;
        this.idKasir         = idKasir;
        this.totalModal      = jumlahBeli * hargaBeliSatuan;
    }

    // ── Getter & Setter ───────────────────────────────────────

    public int  getIdPembelian()               { return idPembelian; }
    public void setIdPembelian(int idPembelian) { this.idPembelian = idPembelian; }

    public LocalDateTime getTanggal()                  { return tanggal; }
    public void          setTanggal(LocalDateTime t)   { this.tanggal = t; }

    public String getIdSupplier()                  { return idSupplier; }
    public void   setIdSupplier(String idSupplier) { this.idSupplier = idSupplier; }

    public String getIdBarang()                { return idBarang; }
    public void   setIdBarang(String idBarang) { this.idBarang = idBarang; }

    public int  getJumlahBeli()              { return jumlahBeli; }
    public void setJumlahBeli(int jumlahBeli) { this.jumlahBeli = jumlahBeli; }

    public double getHargaBeliSatuan()                   { return hargaBeliSatuan; }
    public void   setHargaBeliSatuan(double hargaBeliSatuan) { this.hargaBeliSatuan = hargaBeliSatuan; }

    public double getTotalModal()              { return totalModal; }
    public void   setTotalModal(double total)  { this.totalModal = total; }

    public int  getIdKasir()            { return idKasir; }
    public void setIdKasir(int idKasir) { this.idKasir = idKasir; }

    public String getNamaSupplier()                    { return namaSupplier; }
    public void   setNamaSupplier(String namaSupplier) { this.namaSupplier = namaSupplier; }

    public String getNamaBarang()                  { return namaBarang; }
    public void   setNamaBarang(String namaBarang) { this.namaBarang = namaBarang; }

    public String getNamaKasir()               { return namaKasir; }
    public void   setNamaKasir(String namaKasir) { this.namaKasir = namaKasir; }
}
