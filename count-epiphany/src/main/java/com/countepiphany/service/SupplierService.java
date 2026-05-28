package com.countepiphany.service;

import com.countepiphany.dao.SupplierDAO;
import com.countepiphany.model.Supplier;

import java.util.List;
import java.util.Optional;

/**
 * SupplierService — Logika bisnis manajemen supplier.
 */
public class SupplierService {

    private final SupplierDAO supplierDAO = new SupplierDAO();

    public Supplier tambahSupplier(String idSupplier, String namaSupplier,
                                    String alamat, String telepon, String email) {
        if (idSupplier == null || idSupplier.isBlank())
            throw new IllegalArgumentException("ID Supplier tidak boleh kosong.");
        if (namaSupplier == null || namaSupplier.isBlank())
            throw new IllegalArgumentException("Nama supplier tidak boleh kosong.");
        if (supplierDAO.isIdExists(idSupplier))
            throw new IllegalArgumentException("ID Supplier '" + idSupplier + "' sudah digunakan.");

        Supplier supplier = new Supplier(idSupplier, namaSupplier, alamat, telepon, email);
        if (!supplierDAO.save(supplier))
            throw new RuntimeException("Gagal menyimpan supplier.");
        return supplier;
    }

    public void updateSupplier(Supplier supplier) {
        if (supplier == null)
            throw new IllegalArgumentException("Data supplier tidak boleh null.");
        if (!supplierDAO.update(supplier))
            throw new RuntimeException("Gagal mengupdate supplier.");
    }

    public void hapusSupplier(String idSupplier) {
        if (!supplierDAO.delete(idSupplier))
            throw new RuntimeException("Gagal menghapus supplier '" + idSupplier + "'.");
    }

    public Optional<Supplier> cariById(String idSupplier) {
        return supplierDAO.findById(idSupplier);
    }

    public List<Supplier> cariByNama(String keyword) {
        if (keyword == null || keyword.isBlank()) return supplierDAO.findAll();
        return supplierDAO.findByNama(keyword);
    }

    public List<Supplier> getAllSupplier() {
        return supplierDAO.findAll();
    }
}
