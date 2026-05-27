package com.countepiphany.model;

/**
 * Model: Kasir — merepresentasikan petugas kasir/admin dalam sistem.
 */
public class Kasir {

    private int    idKasir;
    private String namaKasir;
    private String username;
    private String password;   // disimpan sebagai BCrypt hash

    // ── Konstruktor ──────────────────────────────────────────

    public Kasir() {}

    public Kasir(int idKasir, String namaKasir, String username, String password) {
        this.idKasir   = idKasir;
        this.namaKasir = namaKasir;
        this.username  = username;
        this.password  = password;
    }

    /** Konstruktor tanpa ID — untuk pembuatan data baru. */
    public Kasir(String namaKasir, String username, String password) {
        this.namaKasir = namaKasir;
        this.username  = username;
        this.password  = password;
    }

    // ── Getter & Setter ───────────────────────────────────────

    public int getIdKasir()               { return idKasir; }
    public void setIdKasir(int idKasir)   { this.idKasir = idKasir; }

    public String getNamaKasir()                  { return namaKasir; }
    public void   setNamaKasir(String namaKasir)  { this.namaKasir = namaKasir; }

    public String getUsername()               { return username; }
    public void   setUsername(String username){ this.username = username; }

    public String getPassword()               { return password; }
    public void   setPassword(String password){ this.password = password; }

    @Override
    public String toString() {
        return "Kasir{id=" + idKasir + ", nama='" + namaKasir + "', username='" + username + "'}";
    }
}
