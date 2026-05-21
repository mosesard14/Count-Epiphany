package com.countepiphany.controller;

import com.countepiphany.MainApp;
import com.countepiphany.service.AuthService;
import com.countepiphany.service.LaporanService;
import com.countepiphany.util.AlertUtil;
import com.countepiphany.util.CurrencyUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * LaporanController — Controller halaman Laporan Keuangan.
 */
public class LaporanController {

    // ── Hitung Pendapatan
    @FXML private DatePicker dpPendapatanDari;
    @FXML private DatePicker dpPendapatanSampai;
    @FXML private Label      lblTotalPendapatan;

    // ── Laporan per barang
    @FXML private DatePicker dpLaporanDari;
    @FXML private DatePicker dpLaporanSampai;

    @FXML private TableView<Object[]>                tblLaporan;
    @FXML private TableColumn<Object[], String>      colNo;
    @FXML private TableColumn<Object[], String>      colKode;
    @FXML private TableColumn<Object[], String>      colKategori;
    @FXML private TableColumn<Object[], String>      colNama;
    @FXML private TableColumn<Object[], String>      colStok;
    @FXML private TableColumn<Object[], String>      colTerjual;
    @FXML private TableColumn<Object[], String>      colHargaJual;
    @FXML private TableColumn<Object[], String>      colNominal;

    @FXML private Label lblTotalLaporan;

    private final LaporanService laporanService = new LaporanService();
    private final AuthService    authService    = new AuthService();

    private final ObservableList<Object[]> laporanList = FXCollections.observableArrayList();
    private List<Object[]> currentData = new ArrayList<>();
    private double currentTotal = 0;

    @FXML
    public void initialize() {
        setupTable();
        tblLaporan.setItems(laporanList);

        LocalDate now = LocalDate.now();
        dpPendapatanDari.setValue(now.withDayOfMonth(1));
        dpPendapatanSampai.setValue(now);
        dpLaporanDari.setValue(now.withDayOfMonth(1));
        dpLaporanSampai.setValue(now);
    }

    private void setupTable() {
        colNo.setCellValueFactory(cd -> {
            int idx = tblLaporan.getItems().indexOf(cd.getValue()) + 1;
            return new SimpleStringProperty(String.valueOf(idx));
        });
        colKode.setCellValueFactory(cd ->
                new SimpleStringProperty(String.valueOf(cd.getValue()[0])));
        colKategori.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue()[1] == null ? "-" : String.valueOf(cd.getValue()[1])));
        colNama.setCellValueFactory(cd ->
                new SimpleStringProperty(String.valueOf(cd.getValue()[2])));
        colStok.setCellValueFactory(cd ->
                new SimpleStringProperty(String.valueOf(cd.getValue()[3])));
        colTerjual.setCellValueFactory(cd ->
                new SimpleStringProperty(String.valueOf(cd.getValue()[4])));
        colHargaJual.setCellValueFactory(cd ->
                new SimpleStringProperty(CurrencyUtil.format((double) cd.getValue()[5])));
        colNominal.setCellValueFactory(cd ->
                new SimpleStringProperty(CurrencyUtil.format((double) cd.getValue()[6])));
    }

    @FXML
    private void handleHitungPendapatan() {
        LocalDate dari   = dpPendapatanDari.getValue();
        LocalDate sampai = dpPendapatanSampai.getValue();
        if (dari == null || sampai == null) {
            AlertUtil.showWarning("Filter", "Pilih rentang tanggal terlebih dahulu.");
            return;
        }
        double total = laporanService.hitungTotalPendapatan(dari, sampai);
        lblTotalPendapatan.setText(CurrencyUtil.formatPlain(total));
    }

    @FXML
    private void handleTampilLaporan() {
        LocalDate dari   = dpLaporanDari.getValue();
        LocalDate sampai = dpLaporanSampai.getValue();
        if (dari == null || sampai == null) {
            AlertUtil.showWarning("Filter", "Pilih rentang tanggal terlebih dahulu.");
            return;
        }

        currentData = laporanService.getLaporanBarang(dari, sampai);
        laporanList.setAll(currentData);

        currentTotal = currentData.stream()
                .mapToDouble(row -> (double) row[6])
                .sum();
        lblTotalLaporan.setText(CurrencyUtil.formatPlain(currentTotal));
    }

    @FXML
    private void handleExportPdf() {
        if (currentData.isEmpty()) {
            AlertUtil.showWarning("Export", "Tampilkan laporan terlebih dahulu sebelum export.");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Simpan Laporan PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fc.setInitialFileName("laporan_penjualan.pdf");
        File file = fc.showSaveDialog(MainApp.getPrimaryStage());
        if (file != null) {
            try {
                laporanService.exportToPdf(
                        file.getAbsolutePath(),
                        dpLaporanDari.getValue(),
                        dpLaporanSampai.getValue(),
                        currentData,
                        currentTotal);
                AlertUtil.showInfo("Export Berhasil", "Laporan berhasil disimpan ke:\n" + file.getAbsolutePath());
            } catch (IOException e) {
                AlertUtil.showError("Export Gagal", e.getMessage());
            }
        }
    }

    @FXML
    private void handleExportExcel() {
        if (currentData.isEmpty()) {
            AlertUtil.showWarning("Export", "Tampilkan laporan terlebih dahulu sebelum export.");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Simpan Laporan Excel");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        fc.setInitialFileName("laporan_penjualan.xlsx");
        File file = fc.showSaveDialog(MainApp.getPrimaryStage());
        if (file != null) {
            try {
                laporanService.exportToExcel(
                        file.getAbsolutePath(),
                        dpLaporanDari.getValue(),
                        dpLaporanSampai.getValue(),
                        currentData,
                        currentTotal);
                AlertUtil.showInfo("Export Berhasil", "Laporan berhasil disimpan ke:\n" + file.getAbsolutePath());
            } catch (IOException e) {
                AlertUtil.showError("Export Gagal", e.getMessage());
            }
        }
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
