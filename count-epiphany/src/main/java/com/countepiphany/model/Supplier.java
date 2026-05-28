package com.countepiphany.model;

/**
 * Model: Supplier — merepresentasikan pemasok/vendor barang.
 */
public class Supplier {

    private String idSupplier;
    private String namaSupplier;
    private String alamat;
    private String telepon;
    private String email;

    // ── Konstruktor ──────────────────────────────────────────

    public Supplier() {}

    public Supplier(String idSupplier, String namaSupplier,
                    String alamat, String telepon, String email) {
        this.idSupplier   = idSupplier;
        this.namaSupplier = namaSupplier;
        this.alamat       = alamat;
        this.telepon      = telepon;
        this.email        = email;
    }

    // ── Getter & Setter ───────────────────────────────────────

    public String getIdSupplier()                   { return idSupplier; }
    public void   setIdSupplier(String idSupplier)  { this.idSupplier = idSupplier; }

    public String getNamaSupplier()                     { return namaSupplier; }
    public void   setNamaSupplier(String namaSupplier)  { this.namaSupplier = namaSupplier; }

    public String getAlamat()               { return alamat; }
    public void   setAlamat(String alamat)  { this.alamat = alamat; }

    public String getTelepon()                { return telepon; }
    public void   setTelepon(String telepon)  { this.telepon = telepon; }

    public String getEmail()              { return email; }
    public void   setEmail(String email)  { this.email = email; }

    @Override
    public String toString() {
        return namaSupplier + " (" + idSupplier + ")";
    }
}
