package com.countepiphany.controller;

import com.countepiphany.MainApp;
import com.countepiphany.model.DetailTransaksi;
import com.countepiphany.model.Transaksi;
import com.countepiphany.service.AuthService;
import com.countepiphany.service.StrukService;
import com.countepiphany.service.TransaksiService;
import com.countepiphany.util.AlertUtil;
import com.countepiphany.util.CurrencyUtil;
import com.countepiphany.service.BarangService;
import javafx.scene.control.ComboBox;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import com.countepiphany.MainApp;
import java.awt.Desktop;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * TransaksiController — Controller halaman Transaksi Penjualan.
 */
public class TransaksiController {

    // ── Header inputs
    @FXML private ComboBox<String> cmbNamaBarang;
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
    private final BarangService barangService = new BarangService();

    private final ObservableList<DetailTransaksi> keranjangList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        transaksiService.mulaiTransaksiBaru();
        setupTable();
        setupAutocomplete();
        tblKeranjang.setItems(keranjangList);
        txtJumlah.setOnAction(e -> handleTambahBarang());
    }

    private void setupAutocomplete() {
        List<String> daftarNama = barangService.getAllBarang().stream()
                .map(b -> b.getNamaBarang())
                .sorted()
                .collect(Collectors.toList());

        cmbNamaBarang.setItems(FXCollections.observableArrayList(daftarNama));

        // Filter saat mengetik
        cmbNamaBarang.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isBlank()) {
                cmbNamaBarang.setItems(FXCollections.observableArrayList(daftarNama));
                return;
            }
            String keyword = newVal.toLowerCase();
            List<String> filtered = daftarNama.stream()
                    .filter(nama -> nama.toLowerCase().contains(keyword))
                    .collect(Collectors.toList());
            cmbNamaBarang.setItems(FXCollections.observableArrayList(filtered));
            cmbNamaBarang.show(); // buka dropdown otomatis
        });

        cmbNamaBarang.setOnAction(e -> {
            if (cmbNamaBarang.getValue() != null) {
                cmbNamaBarang.getEditor().setText(cmbNamaBarang.getValue());
                txtJumlah.requestFocus();
                txtJumlah.selectAll();
            }
        });
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
        String nama   = cmbNamaBarang.getEditor().getText().trim();
        String jmlStr = txtJumlah.getText().trim();

        if (nama.isEmpty()) {
            AlertUtil.showWarning("Input Kosong", "Pilih atau ketik nama barang.");
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
            DetailTransaksi item = transaksiService.tambahItemKeKeranjang(nama, jumlah);
            if (!keranjangList.contains(item)) {
                keranjangList.add(item);
            }
            tblKeranjang.refresh();
            refreshTotal();
            cmbNamaBarang.getEditor().clear();
            cmbNamaBarang.setValue(null);
            txtJumlah.clear();
            cmbNamaBarang.requestFocus();
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
            FXMLLoader loader = new FXMLLoader(
                MainApp.class.getResource("/com/countepiphany/fxml/pdf_preview.fxml"));
            Parent root = loader.load();
            PdfPreviewController ctrl = loader.getController();

            Stage previewStage = new Stage();
            previewStage.setTitle("Struk Transaksi #" + transaksi.getIdTransaksi());
            previewStage.setScene(new Scene(root));
            previewStage.initOwner(MainApp.getPrimaryStage());
            previewStage.initModality(Modality.NONE);
            previewStage.setResizable(false);
            previewStage.show();

            // Panggil setelah show() agar UI sudah siap
            ctrl.tampilkan(transaksi);
        } catch (IOException e) {
            AlertUtil.showWarning("Struk", "Transaksi berhasil namun preview struk gagal: " + e.getMessage());
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
        cmbNamaBarang.getEditor().clear();
        cmbNamaBarang.setValue(null);
        txtJumlah.clear();
        toggleCashless.setSelected(false);
        toggleCashless.setText("OFF");
        txtNominalBayar.setDisable(false);
    }
}
