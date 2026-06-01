package com.countepiphany.service;

import com.countepiphany.dao.BarangDAO;
import com.countepiphany.dao.TransaksiDAO;
import com.countepiphany.model.Barang;
import com.countepiphany.model.DetailTransaksi;
import com.countepiphany.model.Transaksi;
import com.countepiphany.util.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * TransaksiService — Logika bisnis transaksi penjualan.
 * Mengelola keranjang belanja dan proses pembayaran.
 */
public class TransaksiService {

    private final TransaksiDAO transaksiDAO = new TransaksiDAO();
    private final BarangDAO    barangDAO    = new BarangDAO();

    /** Transaksi yang sedang berjalan (keranjang aktif). */
    private Transaksi transaksiAktif = new Transaksi();

    // ── Manajemen Keranjang ────────────────────────────────

    /**
     * Memulai transaksi baru (reset keranjang).
     */
    public void mulaiTransaksiBaru() {
        transaksiAktif = new Transaksi();
        transaksiAktif.setIdKasir(SessionManager.getCurrentKasirId());
    }

    /**
     * Menambah barang ke keranjang berdasarkan kode/nama barang.
     *
     * @param namaBarang kode barang
     * @param jumlah   kuantitas yang akan ditambahkan
     * @throws IllegalArgumentException jika barang tidak ditemukan atau stok tidak cukup
     */
    public DetailTransaksi tambahItemKeKeranjang(String namaBarang, int jumlah) {
        if (namaBarang == null || namaBarang.isBlank())
            throw new IllegalArgumentException("Kode barang tidak boleh kosong.");
        if (jumlah <= 0)
            throw new IllegalArgumentException("Jumlah harus lebih dari 0.");

        Optional<Barang> opt = barangDAO.findByNama(namaBarang.trim());
        if (opt.isEmpty())
            throw new IllegalArgumentException("Barang '" + namaBarang + "' tidak ditemukan.");

        Barang barang = opt.get();

        // Cek stok — termasuk sudah ada di keranjang
        String idBarang = barang.getIdBarang();
        int sudahDiKeranjang = transaksiAktif.getDetailList().stream()
                .filter(d -> d.getIdBarang().equals(idBarang))
                .mapToInt(DetailTransaksi::getJumlah)
                .sum();

        if (barang.getStok() < sudahDiKeranjang + jumlah)
            throw new IllegalArgumentException(
                    "Stok tidak cukup. Tersedia: " + (barang.getStok() - sudahDiKeranjang));

        // Jika barang sudah ada di keranjang, tambah kuantitasnya
        Optional<DetailTransaksi> existingItem = transaksiAktif.getDetailList().stream()
                .filter(d -> d.getIdBarang().equals(idBarang))
                .findFirst();

        if (existingItem.isPresent()) {
            DetailTransaksi item = existingItem.get();
            item.setJumlah(item.getJumlah() + jumlah);
            return item;
        }

        // Tambah item baru
        DetailTransaksi newItem = new DetailTransaksi(
                barang.getIdBarang(),
                barang.getNamaBarang(),
                jumlah,
                barang.getHargaJual(),
                0   // diskon per item default 0
        );
        transaksiAktif.tambahItem(newItem);
        return newItem;
    }

    /**
     * Menghapus item dari keranjang berdasarkan indeks.
     */
    public void hapusItemDariKeranjang(int index) {
        List<DetailTransaksi> list = transaksiAktif.getDetailList();
        if (index < 0 || index >= list.size())
            throw new IndexOutOfBoundsException("Indeks item tidak valid.");
        list.remove(index);
    }

    /**
     * Menghapus semua item dari keranjang.
     */
    public void kosongkanKeranjang() {
        transaksiAktif.setDetailList(new ArrayList<>());
    }

    /**
     * Mengupdate diskon pada item tertentu di keranjang.
     */
    public void setDiskonItem(int index, double diskon) {
        DetailTransaksi item = transaksiAktif.getDetailList().get(index);
        item.setDiskonItem(diskon);
    }

    // ── Kalkulasi ─────────────────────────────────────────

    /**
     * Menghitung total belanja setelah diskon transaksi.
     */
    public double hitungTotal(double persenDiskon) {
        transaksiAktif.setDiskon(persenDiskon);
        return transaksiAktif.hitungTotal();
    }

    /**
     * Menghitung kembalian dan menyimpannya ke transaksi aktif.
     */
    public double hitungKembalian(double jumlahBayar) {
        transaksiAktif.setJumlahBayar(jumlahBayar);
        return transaksiAktif.hitungKembalian();
    }

    // ── Proses Pembayaran ────────────────────────────────

    /**
     * Memproses pembayaran dan menyimpan transaksi ke database.
     * Stok otomatis berkurang via trigger database.
     *
     * @param jumlahBayar nominal yang diterima dari pelanggan
     * @param metodeBayar cash atau cashless
     * @return Transaksi yang telah disimpan
     */
    public Transaksi prosesPembayaran(double jumlahBayar, Transaksi.MetodeBayar metodeBayar) {
        if (transaksiAktif.getDetailList().isEmpty())
            throw new IllegalStateException("Keranjang belanja kosong.");

        transaksiAktif.hitungTotal();

        if (metodeBayar == Transaksi.MetodeBayar.cash
                && jumlahBayar < transaksiAktif.getTotalHarga()) {
            throw new IllegalArgumentException("Uang yang diterima kurang dari total belanja.");
        }

        transaksiAktif.setJumlahBayar(jumlahBayar);
        transaksiAktif.setMetodeBayar(metodeBayar);
        transaksiAktif.hitungKembalian();
        transaksiAktif.setIdKasir(SessionManager.getCurrentKasirId());
        transaksiAktif.setNamaKasir(SessionManager.getCurrentKasir().getNamaKasir());

        if (!transaksiDAO.saveTransaksiFull(transaksiAktif))
            throw new RuntimeException("Gagal menyimpan transaksi ke database.");

        Transaksi selesai = transaksiAktif;
        mulaiTransaksiBaru();   // reset keranjang
        return selesai;
    }

    // ── Query Riwayat ────────────────────────────────────

    public List<Transaksi> getRiwayatSemua() {
        return transaksiDAO.findAll();
    }

    public List<Transaksi> getRiwayatByPeriode(java.time.LocalDate dari,
                                               java.time.LocalDate sampai) {
        return transaksiDAO.findByPeriode(dari, sampai);
    }

    public Optional<Transaksi> getTransaksiById(int id) {
        return transaksiDAO.findById(id);
    }

    public List<DetailTransaksi> getDetailByTransaksiId(int id) {
        return transaksiDAO.findDetailByTransaksiId(id);
    }

    // ── Getter Transaksi Aktif ───────────────────────────

    public Transaksi getTransaksiAktif() { return transaksiAktif; }

    public List<DetailTransaksi> getKeranjang() {
        return transaksiAktif.getDetailList();
    }
}
