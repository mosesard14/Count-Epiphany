package com.countepiphany.controller;

import com.countepiphany.MainApp;
import com.countepiphany.model.Barang;
import com.countepiphany.model.PembelianBarang;
import com.countepiphany.model.Supplier;
import com.countepiphany.service.AuthService;
import com.countepiphany.service.BarangService;
import com.countepiphany.service.PembelianService;
import com.countepiphany.service.SupplierService;
import com.countepiphany.util.AlertUtil;
import com.countepiphany.util.CurrencyUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PembelianController — Controller halaman Manajemen Pembelian (Restock).
 */
public class PembelianController {

    @FXML private ComboBox<String> cmbSupplier;
    @FXML private ComboBox<String> cmbBarang;
    @FXML private TextField        txtJumlahBeli;
    @FXML private TextField        txtHargaBeliSatuan;
    @FXML private Label            lblTotalModal;

    @FXML private DatePicker dpDari;
    @FXML private DatePicker dpSampai;
    @FXML private Label      lblTotalModalPeriode;

    @FXML private TableView<PembelianBarang>                 tblPembelian;
    @FXML private TableColumn<PembelianBarang, String>       colId;
    @FXML private TableColumn<PembelianBarang, String>       colTanggal;
    @FXML private TableColumn<PembelianBarang, String>       colSupplier;
    @FXML private TableColumn<PembelianBarang, String>       colBarang;
    @FXML private TableColumn<PembelianBarang, String>       colJumlah;
    @FXML private TableColumn<PembelianBarang, String>       colHargaSat;
    @FXML private TableColumn<PembelianBarang, String>       colTotal;
    @FXML private TableColumn<PembelianBarang, String>       colKasir;

    private final PembelianService pembelianService = new PembelianService();
    private final SupplierService  supplierService  = new SupplierService();
    private final BarangService    barangService    = new BarangService();
    private final AuthService      authService      = new AuthService();

    private final ObservableList<PembelianBarang> pembelianList = FXCollections.observableArrayList();
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Peta ID untuk mengambil ID asli dari display string
    private List<Supplier> allSuppliers;
    private List<Barang>   allBarang;

    @FXML
    public void initialize() {
        setupTable();
        setupComboBoxes();
        setupRealTimeCalc();
        tblPembelian.setItems(pembelianList);
        dpDari.setValue(LocalDate.now().withDayOfMonth(1));
        dpSampai.setValue(LocalDate.now());
        muatSemuaPembelian();
    }

    private void setupTable() {
        colId.setCellValueFactory(cd ->
                new SimpleStringProperty(String.valueOf(cd.getValue().getIdPembelian())));
        colTanggal.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getTanggal() != null
                        ? cd.getValue().getTanggal().format(DT_FMT) : "-"));
        colSupplier.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getNamaSupplier() != null
                        ? cd.getValue().getNamaSupplier() : cd.getValue().getIdSupplier()));
        colBarang.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getNamaBarang() != null
                        ? cd.getValue().getNamaBarang() : cd.getValue().getIdBarang()));
        colJumlah.setCellValueFactory(cd ->
                new SimpleStringProperty(String.valueOf(cd.getValue().getJumlahBeli())));
        colHargaSat.setCellValueFactory(cd ->
                new SimpleStringProperty(CurrencyUtil.format(cd.getValue().getHargaBeliSatuan())));
        colTotal.setCellValueFactory(cd ->
                new SimpleStringProperty(CurrencyUtil.format(cd.getValue().getTotalModal())));
        colKasir.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getNamaKasir() != null
                        ? cd.getValue().getNamaKasir() : "-"));
    }

    private void setupComboBoxes() {
        allSuppliers = supplierService.getAllSupplier();
        allBarang    = barangService.getAllBarang();

        List<String> supplierDisplay = allSuppliers.stream()
                .map(s -> s.getIdSupplier() + " - " + s.getNamaSupplier())
                .collect(Collectors.toList());
        cmbSupplier.setItems(FXCollections.observableArrayList(supplierDisplay));

        List<String> barangDisplay = allBarang.stream()
                .map(b -> b.getIdBarang() + " - " + b.getNamaBarang()
                        + " (Stok: " + b.getStok() + ")")
                .collect(Collectors.toList());
        cmbBarang.setItems(FXCollections.observableArrayList(barangDisplay));
    }

    private void setupRealTimeCalc() {
        // Hitung total modal secara real-time
        txtJumlahBeli.setOnKeyReleased(e -> hitungTotalModal());
        txtHargaBeliSatuan.setOnKeyReleased(e -> hitungTotalModal());
    }

    private void hitungTotalModal() {
        try {
            int jumlah = Integer.parseInt(txtJumlahBeli.getText().trim());
            double harga = Double.parseDouble(txtHargaBeliSatuan.getText().trim().replace(",", "."));
            lblTotalModal.setText(CurrencyUtil.format(jumlah * harga));
        } catch (NumberFormatException ignored) {
            lblTotalModal.setText("Rp 0,00");
        }
    }

    @FXML
    private void handleSimpanPembelian() {
        try {
            if (cmbSupplier.getValue() == null)
                throw new IllegalArgumentException("Pilih supplier terlebih dahulu.");
            if (cmbBarang.getValue() == null)
                throw new IllegalArgumentException("Pilih barang terlebih dahulu.");

            String idSupplier = cmbSupplier.getValue().split(" - ")[0];
            String idBarang   = cmbBarang.getValue().split(" - ")[0];
            int jumlah        = Integer.parseInt(txtJumlahBeli.getText().trim());
            double harga      = Double.parseDouble(txtHargaBeliSatuan.getText().trim().replace(",", "."));

            PembelianBarang pb = pembelianService.catatPembelian(idSupplier, idBarang, jumlah, harga);
            AlertUtil.showInfo("Berhasil",
                    "Pembelian berhasil dicatat.\nStok barang telah bertambah " + jumlah + " unit.");
            handleBatal();
            muatSemuaPembelian();

        } catch (NumberFormatException e) {
            AlertUtil.showWarning("Input Salah", "Jumlah dan harga harus berupa angka valid.");
        } catch (IllegalArgumentException e) {
            AlertUtil.showWarning("Validasi Gagal", e.getMessage());
        } catch (Exception e) {
            AlertUtil.showError("Error", e.getMessage());
        }
    }

    @FXML
    private void handleBatal() {
        cmbSupplier.setValue(null);
        cmbBarang.setValue(null);
        txtJumlahBeli.clear();
        txtHargaBeliSatuan.clear();
        lblTotalModal.setText("Rp 0,00");
    }

    @FXML
    private void handleFilter() {
        LocalDate dari   = dpDari.getValue();
        LocalDate sampai = dpSampai.getValue();
        if (dari == null || sampai == null) {
            AlertUtil.showWarning("Filter", "Pilih tanggal mulai dan selesai.");
            return;
        }
        List<PembelianBarang> hasil = pembelianService.getPembelianByPeriode(dari, sampai);
        pembelianList.setAll(hasil);

        double totalModal = pembelianService.getTotalModalByPeriode(dari, sampai);
        lblTotalModalPeriode.setText(CurrencyUtil.format(totalModal));
    }

    @FXML
    private void handleTampilSemua() {
        muatSemuaPembelian();
    }

    private void muatSemuaPembelian() {
        List<PembelianBarang> all = pembelianService.getRiwayatPembelian();
        pembelianList.setAll(all);
        double total = all.stream().mapToDouble(PembelianBarang::getTotalModal).sum();
        lblTotalModalPeriode.setText(CurrencyUtil.format(total));
    }

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
}
