package com.countepiphany.model;

/**
 * Model: DetailTransaksi — satu baris item dalam keranjang belanja.
 * Menyimpan snapshot harga saat transaksi terjadi untuk integritas historis.
 */
public class DetailTransaksi {

    private int    idDetail;
    private int    idTransaksi;
    private String idBarang;
    private int    jumlah;
    private double hargaSatuan;   // snapshot harga saat transaksi
    private double diskonItem;    // diskon per item dalam persen (0-100)
    private double subtotal;

    // Field join untuk tampilan
    private String namaBarang;
    private String kodeBarang;    // alias untuk idBarang dalam tampilan

    // ── Konstruktor ──────────────────────────────────────────

    public DetailTransaksi() {}

    /**
     * Konstruktor untuk item baru saat kasir menambah barang ke keranjang.
     */
    public DetailTransaksi(String idBarang, String namaBarang,
                           int jumlah, double hargaSatuan, double diskonItem) {
        this.idBarang    = idBarang;
        this.namaBarang  = namaBarang;
        this.kodeBarang  = idBarang;
        this.jumlah      = jumlah;
        this.hargaSatuan = hargaSatuan;
        this.diskonItem  = diskonItem;
        this.subtotal    = hitungSubtotal();
    }

    // ── Business Logic ───────────────────────────────────────

    /**
     * Menghitung subtotal: jumlah × hargaSatuan × (1 - diskonItem/100).
     */
    public double hitungSubtotal() {
        subtotal = jumlah * hargaSatuan * (1 - diskonItem / 100.0);
        return subtotal;
    }

    // ── Getter & Setter ───────────────────────────────────────

    public int  getIdDetail()               { return idDetail; }
    public void setIdDetail(int idDetail)   { this.idDetail = idDetail; }

    public int  getIdTransaksi()                  { return idTransaksi; }
    public void setIdTransaksi(int idTransaksi)   { this.idTransaksi = idTransaksi; }

    public String getIdBarang()                { return idBarang; }
    public void   setIdBarang(String idBarang) { this.idBarang = idBarang; }

    public int  getJumlah()          { return jumlah; }
    public void setJumlah(int jumlah) {
        this.jumlah = jumlah;
        hitungSubtotal();
    }

    public double getHargaSatuan()                 { return hargaSatuan; }
    public void   setHargaSatuan(double hargaSatuan) {
        this.hargaSatuan = hargaSatuan;
        hitungSubtotal();
    }

    public double getDiskonItem()              { return diskonItem; }
    public void   setDiskonItem(double diskon) {
        this.diskonItem = diskon;
        hitungSubtotal();
    }

    public double getSubtotal()              { return subtotal; }
    public void   setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public String getNamaBarang()                  { return namaBarang; }
    public void   setNamaBarang(String namaBarang) { this.namaBarang = namaBarang; }

    public String getKodeBarang()                  { return kodeBarang; }
    public void   setKodeBarang(String kodeBarang) { this.kodeBarang = kodeBarang; }
}
