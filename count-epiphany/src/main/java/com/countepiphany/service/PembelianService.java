package com.countepiphany.service;

import com.countepiphany.dao.BarangDAO;
import com.countepiphany.dao.PembelianBarangDAO;
import com.countepiphany.dao.SupplierDAO;
import com.countepiphany.model.PembelianBarang;
import com.countepiphany.util.SessionManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * PembelianService — Logika bisnis pencatatan pembelian/restock barang.
 * Ini satu-satunya jalur yang diizinkan untuk menambah stok barang.
 */
public class PembelianService {

    private final PembelianBarangDAO pembelianDAO = new PembelianBarangDAO();
    private final BarangDAO          barangDAO    = new BarangDAO();
    private final SupplierDAO        supplierDAO  = new SupplierDAO();

    /**
     * Mencatat pembelian barang dari supplier dan otomatis menambah stok.
     * Trigger database yang menangani penambahan stok pada tabel barang.
     *
     * @throws IllegalArgumentException jika data tidak valid
     */
    public PembelianBarang catatPembelian(String idSupplier, String idBarang,
                                           int jumlahBeli, double hargaBeliSatuan) {
        // Validasi
        if (idSupplier == null || idSupplier.isBlank())
            throw new IllegalArgumentException("Supplier harus dipilih.");
        if (idBarang == null || idBarang.isBlank())
            throw new IllegalArgumentException("Barang harus dipilih.");
        if (jumlahBeli <= 0)
            throw new IllegalArgumentException("Jumlah beli harus lebih dari 0.");
        if (hargaBeliSatuan < 0)
            throw new IllegalArgumentException("Harga beli tidak boleh negatif.");

        // Verifikasi supplier dan barang exist
        if (!supplierDAO.findById(idSupplier).isPresent())
            throw new IllegalArgumentException("Supplier '" + idSupplier + "' tidak ditemukan.");
        if (!barangDAO.findById(idBarang).isPresent())
            throw new IllegalArgumentException("Barang '" + idBarang + "' tidak ditemukan.");

        PembelianBarang pembelian = new PembelianBarang(
                idSupplier, idBarang, jumlahBeli, hargaBeliSatuan,
                SessionManager.getCurrentKasirId()
        );

        if (!pembelianDAO.save(pembelian))
            throw new RuntimeException("Gagal menyimpan data pembelian.");

        // Update harga beli terbaru di tabel barang (opsional, update ke harga terbaru)
        barangDAO.findById(idBarang).ifPresent(b -> {
            b.setHargaBeli(hargaBeliSatuan);
            barangDAO.update(b);
        });

        return pembelian;
    }

    public List<PembelianBarang> getRiwayatPembelian() {
        return pembelianDAO.findAll();
    }

    public List<PembelianBarang> getPembelianByPeriode(LocalDate dari, LocalDate sampai) {
        return pembelianDAO.findByPeriode(dari, sampai);
    }

    public double getTotalModalByPeriode(LocalDate dari, LocalDate sampai) {
        return pembelianDAO.sumTotalModalByPeriode(dari, sampai);
    }
}
