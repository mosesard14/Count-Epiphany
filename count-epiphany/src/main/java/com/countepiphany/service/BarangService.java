package com.countepiphany.service;

import com.countepiphany.dao.BarangDAO;
import com.countepiphany.model.Barang;

import java.util.List;
import java.util.Optional;

/**
 * BarangService — Logika bisnis manajemen inventori barang.
 */
public class BarangService {

    private final BarangDAO barangDAO = new BarangDAO();

    /**
     * Menambah barang baru ke inventori.
     * Stok awal bisa diset 0; penambahan stok via PembelianService.
     *
     * @throws IllegalArgumentException jika data tidak valid atau ID sudah ada
     */
    public Barang tambahBarang(String idBarang, String namaBarang,
                                double hargaBeli, double hargaJual,
                                String kategori, String idSupplier,
                                int stokMinimum) {
        if (idBarang == null || idBarang.isBlank())
            throw new IllegalArgumentException("Kode barang tidak boleh kosong.");
        if (namaBarang == null || namaBarang.isBlank())
            throw new IllegalArgumentException("Nama barang tidak boleh kosong.");
        if (hargaBeli < 0 || hargaJual < 0)
            throw new IllegalArgumentException("Harga tidak boleh negatif.");
        if (hargaJual < hargaBeli)
            throw new IllegalArgumentException("Harga jual tidak boleh lebih kecil dari harga beli.");
        if (barangDAO.isIdExists(idBarang))
            throw new IllegalArgumentException("Kode barang '" + idBarang + "' sudah digunakan.");

        Barang barang = new Barang(idBarang, namaBarang, hargaBeli, hargaJual,
                                   0, kategori, idSupplier, stokMinimum);
        if (!barangDAO.save(barang))
            throw new RuntimeException("Gagal menyimpan barang ke database.");
        return barang;
    }

    /**
     * Mengupdate informasi barang (tidak mengubah stok).
     */
    public void updateBarang(Barang barang) {
        if (barang == null)
            throw new IllegalArgumentException("Data barang tidak boleh null.");
        if (!barangDAO.update(barang))
            throw new RuntimeException("Gagal mengupdate barang.");
    }

    /**
     * Menghapus barang dari inventori.
     */
    public void hapusBarang(String idBarang) {
        if (!barangDAO.delete(idBarang))
            throw new RuntimeException("Gagal menghapus barang '" + idBarang + "'.");
    }

    /** Mencari barang berdasarkan kode atau nama. */
    public Optional<Barang> cariBarangById(String idBarang) {
        return barangDAO.findById(idBarang);
    }

    /** Mencari barang berdasarkan keyword (nama atau kode). */
    public List<Barang> cariBarang(String keyword) {
        if (keyword == null || keyword.isBlank()) return barangDAO.findAll();
        return barangDAO.findByKeyword(keyword);
    }

    /** Mengambil semua barang. */
    public List<Barang> getAllBarang() {
        return barangDAO.findAll();
    }

    /** Filter barang berdasarkan kategori. */
    public List<Barang> getBarangByKategori(String kategori) {
        if (kategori == null || kategori.isBlank() || kategori.equals("Semua"))
            return barangDAO.findAll();
        return barangDAO.findByKategori(kategori);
    }

    /** Mendapatkan semua kategori untuk dropdown. */
    public List<String> getAllKategori() {
        return barangDAO.findAllKategori();
    }

    /**
     * Mendapatkan daftar barang dengan stok di bawah minimum.
     * Digunakan untuk notifikasi peringatan stok rendah.
     */
    public List<Barang> getBarangStokRendah() {
        return barangDAO.findStokRendah();
    }

    /** Mencari barang dengan kombinasi filter keyword dan kategori. */
    public List<Barang> filterBarang(String keyword, String kategori, String filterStok) {
        List<Barang> all;

        if (keyword != null && !keyword.isBlank()) {
            all = barangDAO.findByKeyword(keyword);
        } else if (kategori != null && !kategori.isBlank() && !kategori.equals("Semua")) {
            all = barangDAO.findByKategori(kategori);
        } else {
            all = barangDAO.findAll();
        }

        // Filter stok
        if ("Habis".equals(filterStok)) {
            all = all.stream().filter(b -> b.getStok() == 0).toList();
        } else if ("Rendah".equals(filterStok)) {
            all = all.stream().filter(Barang::isStokRendah).toList();
        }

        return all;
    }
}
