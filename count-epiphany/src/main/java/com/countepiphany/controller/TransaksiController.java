package com.countepiphany.controller;

import com.countepiphany.MainApp;
import com.countepiphany.model.DetailTransaksi;
import com.countepiphany.model.Transaksi;
import com.countepiphany.service.AuthService;
import com.countepiphany.service.StrukService;
import com.countepiphany.service.TransaksiService;
import com.countepiphany.util.AlertUtil;
import com.countepiphany.util.CurrencyUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * TransaksiController — Controller halaman Transaksi Penjualan.
 */
public class TransaksiController {

    // ── Header inputs
    @FXML private TextField   txtKodeBarang;
    @FXML private TextField   txtNamaBarangCari;
    @FXML private TextField   txtJumlah;

    // ── Total display
    @FXML private Label lblTotal;

    // ── Table keranjang
    @FXML private TableView<DetailTransaksi>        tblKeranjang;
    @FXML private TableColumn<DetailTransaksi, String> colNo;
    @FXML private TableColumn<DetailTransaksi, String> colKode;
    @FXML private TableColumn<DetailTransaksi, String> colNama;
    @FXML private TableColumn<DetailTransaksi, String> colHarga;
    @FXML private TableColumn<DetailTransaksi, String> colQty;
    @FXML private TableColumn<DetailTransaksi, String> colDiskon;
    @FXML private TableColumn<DetailTransaksi, String> colSubtotal;
    @FXML private TableColumn<DetailTransaksi, Void>   colAksi;

    // ── Diskon
    @FXML private TextField txtDiskon;

    // ── Kembalian
    @FXML private ToggleButton toggleCashless;
    @FXML private TextField    txtNominalBayar;
    @FXML private Label        lblKembalian;

    // ── Bayar
    @FXML private Button btnBayar;

    private final TransaksiService transaksiService = new TransaksiService();
    private final AuthService      authService      = new AuthService();
    private final StrukService     strukService     = new StrukService();

    private final ObservableList<DetailTransaksi> keranjangList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        transaksiService.mulaiTransaksiBaru();
        setupTable();
        tblKeranjang.setItems(keranjangList);

        // Sinkronisasi pencarian: kode atau nama
        txtKodeBarang.setOnAction(e -> handleTambahBarang());
        txtNamaBarangCari.setOnAction(e -> handleTambahBarang());
        txtJumlah.setOnAction(e -> handleTambahBarang());
    }

    // ── Table Setup ───────────────────────────────────────

    private void setupTable() {
        colNo.setCellValueFactory(cd -> {
            int idx = tblKeranjang.getItems().indexOf(cd.getValue()) + 1;
            return new SimpleStringProperty(String.valueOf(idx));
        });
        colKode.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getIdBarang()));
        colNama.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getNamaBarang()));
        colHarga.setCellValueFactory(cd ->
                new SimpleStringProperty(CurrencyUtil.format(cd.getValue().getHargaSatuan())));
        colQty.setCellValueFactory(cd ->
                new SimpleStringProperty(String.valueOf(cd.getValue().getJumlah())));
        colDiskon.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getDiskonItem() + "%"));
        colSubtotal.setCellValueFactory(cd ->
                new SimpleStringProperty(CurrencyUtil.format(cd.getValue().getSubtotal())));

        // Kolom aksi: tombol hapus
        colAksi.setCellFactory(col -> new TableCell<>() {
            private final Button btnHapus = new Button("Hapus");
            {
                btnHapus.getStyleClass().add("btn-danger");
                btnHapus.setStyle("-fx-font-size:11px;");
                btnHapus.setOnAction(e -> {
                    int idx = getIndex();
                    if (idx >= 0 && idx < keranjangList.size()) {
                        keranjangList.remove(idx);
                        transaksiService.hapusItemDariKeranjang(idx);
                        refreshTotal();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnHapus);
            }
        });
    }

    // ── Event Handlers ────────────────────────────────────

    @FXML
    private void handleTambahBarang() {
        String kode   = txtKodeBarang.getText().trim();
        String nama   = txtNamaBarangCari.getText().trim();
        String jmlStr = txtJumlah.getText().trim();

        String idBarang = kode.isEmpty() ? nama : kode;
        if (idBarang.isEmpty()) {
            AlertUtil.showWarning("Input Kosong", "Masukkan kode atau nama barang.");
            return;
        }

        int jumlah = 1;
        if (!jmlStr.isEmpty()) {
            try {
                jumlah = Integer.parseInt(jmlStr);
            } catch (NumberFormatException ex) {
                AlertUtil.showWarning("Input Salah", "Jumlah harus berupa angka.");
                return;
            }
        }

        try {
            DetailTransaksi item = transaksiService.tambahItemKeKeranjang(idBarang, jumlah);
            // Jika item sudah ada, update di list; jika baru, tambahkan
            if (!keranjangList.contains(item)) {
                keranjangList.add(item);
            }
            tblKeranjang.refresh();
            refreshTotal();
            txtKodeBarang.clear();
            txtNamaBarangCari.clear();
            txtJumlah.clear();
            txtKodeBarang.requestFocus();
        } catch (IllegalArgumentException ex) {
            AlertUtil.showWarning("Gagal Menambah", ex.getMessage());
        }
    }

    @FXML
    private void handleHapusSemua() {
        if (keranjangList.isEmpty()) return;
        if (AlertUtil.showConfirmation("Konfirmasi", "Hapus semua item dari keranjang?")) {
            keranjangList.clear();
            transaksiService.kosongkanKeranjang();
            refreshTotal();
            lblKembalian.setText("Rp 0,00");
        }
    }

    @FXML
    private void handleTerapkanDiskon() {
        try {
            double diskon = Double.parseDouble(txtDiskon.getText().trim());
            if (diskon < 0 || diskon > 100) throw new NumberFormatException();
            transaksiService.hitungTotal(diskon);
            refreshTotal();
        } catch (NumberFormatException e) {
            AlertUtil.showWarning("Diskon Tidak Valid", "Masukkan persentase diskon antara 0-100.");
        }
    }

    @FXML
    private void handleToggleCashless() {
        boolean isCashless = toggleCashless.isSelected();
        toggleCashless.setText(isCashless ? "ON" : "OFF");
        txtNominalBayar.setDisable(isCashless);
        if (isCashless) {
            txtNominalBayar.setText("");
            lblKembalian.setText("Rp 0,00");
        }
    }

    @FXML
    private void hitungKembalianRealtime() {
        try {
            double bayar = Double.parseDouble(txtNominalBayar.getText().trim().replace(",", "."));
            double kembalian = transaksiService.hitungKembalian(bayar);
            lblKembalian.setText(CurrencyUtil.format(Math.max(0, kembalian)));
            lblKembalian.setStyle(kembalian < 0
                    ? "-fx-text-fill: #E53935; -fx-font-weight: bold;"
                    : "-fx-text-fill: #43A047; -fx-font-weight: bold;");
        } catch (NumberFormatException ignored) {
            lblKembalian.setText("Rp 0,00");
        }
    }

    @FXML
    private void handleBayar() {
        if (keranjangList.isEmpty()) {
            AlertUtil.showWarning("Keranjang Kosong", "Tambahkan barang terlebih dahulu.");
            return;
        }

        Transaksi.MetodeBayar metode = toggleCashless.isSelected()
                ? Transaksi.MetodeBayar.cashless
                : Transaksi.MetodeBayar.cash;

        double bayar;
        if (metode == Transaksi.MetodeBayar.cashless) {
            bayar = transaksiService.getTransaksiAktif().getTotalHarga();
        } else {
            try {
                bayar = Double.parseDouble(txtNominalBayar.getText().trim().replace(",", "."));
            } catch (NumberFormatException e) {
                AlertUtil.showWarning("Input Salah", "Masukkan nominal uang yang diterima.");
                return;
            }
        }

        try {
            double diskon = 0;
            try { diskon = Double.parseDouble(txtDiskon.getText().trim()); } catch (Exception ignored) {}
            transaksiService.hitungTotal(diskon);

            Transaksi selesai = transaksiService.prosesPembayaran(bayar, metode);
            AlertUtil.showInfo("Transaksi Berhasil",
                    "Transaksi #" + selesai.getIdTransaksi() + " berhasil disimpan.\n"
                    + "Total: " + CurrencyUtil.format(selesai.getTotalHarga()) + "\n"
                    + "Kembalian: " + CurrencyUtil.format(selesai.getKembalian()));

            // Cetak struk ke file temp
            cetakStrukOtomatis(selesai);

            // Reset UI
            resetUI();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            AlertUtil.showWarning("Gagal Bayar", ex.getMessage());
        } catch (Exception ex) {
            AlertUtil.showError("Error", "Terjadi kesalahan: " + ex.getMessage());
        }
    }

    @FXML
    private void handleBatalkan() {
        if (keranjangList.isEmpty()) return;
        if (AlertUtil.showConfirmation("Batalkan Transaksi",
                "Yakin ingin membatalkan transaksi ini?")) {
            resetUI();
        }
    }

    private void cetakStrukOtomatis(Transaksi transaksi) {
        try {
            String path = System.getProperty("java.io.tmpdir")
                    + File.separator + "struk_" + transaksi.getIdTransaksi() + ".pdf";
            strukService.cetakStruk(transaksi, path);
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(new File(path));
            }
        } catch (IOException e) {
            AlertUtil.showWarning("Struk", "Transaksi berhasil namun gagal mencetak struk: " + e.getMessage());
        }
    }

    // ── Navigation ────────────────────────────────────────

    @FXML private void gotoTransaksi() { MainApp.navigateTo("transaksi.fxml"); }
    @FXML private void gotoInventori() { MainApp.navigateTo("inventori.fxml"); }
    @FXML private void gotoSupplier()  { MainApp.navigateTo("supplier.fxml"); }
    @FXML private void gotoLaporan()   { MainApp.navigateTo("laporan.fxml"); }
    @FXML private void gotoRiwayat()   { MainApp.navigateTo("riwayat.fxml"); }
    @FXML private void gotoPembelian() { MainApp.navigateTo("pembelian.fxml"); }

    @FXML
    private void handleLogout() {
        if (AlertUtil.showConfirmation("Logout", "Yakin ingin keluar?")) {
            authService.logout();
            MainApp.navigateTo("login.fxml");
        }
    }

    // ── Helpers ───────────────────────────────────────────

    private void refreshTotal() {
        double diskon = 0;
        try { diskon = Double.parseDouble(txtDiskon.getText().trim()); } catch (Exception ignored) {}
        double total = transaksiService.hitungTotal(diskon);
        lblTotal.setText(CurrencyUtil.formatPlain(total));
        hitungKembalianRealtime();
    }

    private void resetUI() {
        keranjangList.clear();
        transaksiService.mulaiTransaksiBaru();
        lblTotal.setText("0,00");
        lblKembalian.setText("Rp 0,00");
        txtDiskon.clear();
        txtNominalBayar.clear();
        txtKodeBarang.clear();
        txtNamaBarangCari.clear();
        txtJumlah.clear();
        toggleCashless.setSelected(false);
        toggleCashless.setText("OFF");
        txtNominalBayar.setDisable(false);
    }
}
