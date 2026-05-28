package com.countepiphany.model;

/**
 * Model: Barang — merepresentasikan produk/item dalam inventori.
 * Revisi: tambah field subkategori untuk hierarki dua level
 *         kategori (induk) -> subkategori (jenis).
 *
 * Contoh:
 *   kategori   = "Minuman"
 *   subkategori = "Susu"
 *   namaBarang  = "Susu Kambing Etawa 200ml"
 */
public class Barang {

    private String  idBarang;
    private String  namaBarang;
    private double  hargaBeli;
    private double  hargaJual;
    private int     stok;
    private String  kategori;
    private String  subkategori;   // <-- baru
    private String  idSupplier;
    private int     stokMinimum;

    // ── Konstruktor ──────────────────────────────────────────

    public Barang() {
        this.stokMinimum = 5;
    }

    public Barang(String idBarang, String namaBarang, double hargaBeli,
                  double hargaJual, int stok, String kategori,
                  String idSupplier, int stokMinimum) {
        this.idBarang    = idBarang;
        this.namaBarang  = namaBarang;
        this.hargaBeli   = hargaBeli;
        this.hargaJual   = hargaJual;
        this.stok        = stok;
        this.kategori    = kategori;
        this.idSupplier  = idSupplier;
        this.stokMinimum = stokMinimum;
    }

    public Barang(String idBarang, String namaBarang, double hargaBeli,
                  double hargaJual, int stok, String kategori, String subkategori,
                  String idSupplier, int stokMinimum) {
        this(idBarang, namaBarang, hargaBeli, hargaJual, stok, kategori, idSupplier, stokMinimum);
        this.subkategori = subkategori;
    }

    // ── Helper ───────────────────────────────────────────────

    public boolean isStokRendah() {
        return stok <= stokMinimum;
    }

    // ── Getter & Setter ───────────────────────────────────────

    public String getIdBarang()                  { return idBarang; }
    public void   setIdBarang(String idBarang)   { this.idBarang = idBarang; }

    public String getNamaBarang()                    { return namaBarang; }
    public void   setNamaBarang(String namaBarang)   { this.namaBarang = namaBarang; }

    public double getHargaBeli()                { return hargaBeli; }
    public void   setHargaBeli(double hargaBeli) { this.hargaBeli = hargaBeli; }

    public double getHargaJual()                { return hargaJual; }
    public void   setHargaJual(double hargaJual) { this.hargaJual = hargaJual; }

    public int  getStok()          { return stok; }
    public void setStok(int stok)  { this.stok = stok; }

    public String getKategori()                { return kategori; }
    public void   setKategori(String kategori) { this.kategori = kategori; }

    public String getSubkategori()                   { return subkategori; }
    public void   setSubkategori(String subkategori) { this.subkategori = subkategori; }

    public String getIdSupplier()                  { return idSupplier; }
    public void   setIdSupplier(String idSupplier) { this.idSupplier = idSupplier; }

    public int  getStokMinimum()                 { return stokMinimum; }
    public void setStokMinimum(int stokMinimum)  { this.stokMinimum = stokMinimum; }

    @Override
    public String toString() {
        return namaBarang + " [" + idBarang + "]";
    }
}
