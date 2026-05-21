package com.countepiphany.controller;

import com.countepiphany.MainApp;
import com.countepiphany.model.DetailTransaksi;
import com.countepiphany.model.Transaksi;
import com.countepiphany.service.AuthService;
import com.countepiphany.service.TransaksiService;
import com.countepiphany.util.AlertUtil;
import com.countepiphany.util.CurrencyUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * RiwayatController — Controller halaman Riwayat Transaksi Penjualan.
 * Mendukung drill-down ke detail per transaksi.
 */
public class RiwayatController {

    // ── Views
    @FXML private VBox viewDaftar;
    @FXML private VBox viewDetail;

    // ── Filter
    @FXML private DatePicker dpDari;
    @FXML private DatePicker dpSampai;

    // ── Tabel Riwayat
    @FXML private TableView<Transaksi>                 tblRiwayat;
    @FXML private TableColumn<Transaksi, String>       colNo;
    @FXML private TableColumn<Transaksi, String>       colTanggal;
    @FXML private TableColumn<Transaksi, String>       colWaktu;
    @FXML private TableColumn<Transaksi, String>       colId;
    @FXML private TableColumn<Transaksi, String>       colMetode;
    @FXML private TableColumn<Transaksi, String>       colDiskon;
    @FXML private TableColumn<Transaksi, String>       colTotal;
    @FXML private TableColumn<Transaksi, Void>         colAksi;

    // ── Detail Transaksi header labels
    @FXML private Label lblDetTanggal;
    @FXML private Label lblDetWaktu;
    @FXML private Label lblDetId;
    @FXML private Label lblDetTotal;
    @FXML private Label lblDetTotalBawah;

    // ── Tabel Detail
    @FXML private TableView<DetailTransaksi>                 tblDetail;
    @FXML private TableColumn<DetailTransaksi, String>       colDetNo;
    @FXML private TableColumn<DetailTransaksi, String>       colDetKode;
    @FXML private TableColumn<DetailTransaksi, String>       colDetNama;
    @FXML private TableColumn<DetailTransaksi, String>       colDetHarga;
    @FXML private TableColumn<DetailTransaksi, String>       colDetQty;
    @FXML private TableColumn<DetailTransaksi, String>       colDetDiskon;
    @FXML private TableColumn<DetailTransaksi, String>       colDetSubtotal;

    private final TransaksiService transaksiService = new TransaksiService();
    private final AuthService      authService      = new AuthService();

    private final ObservableList<Transaksi>      riwayatList = FXCollections.observableArrayList();
    private final ObservableList<DetailTransaksi> detailList  = FXCollections.observableArrayList();

    private static final DateTimeFormatter TGL_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter WKT_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML
    public void initialize() {
        setupTabelRiwayat();
        setupTabelDetail();
        tblRiwayat.setItems(riwayatList);
        tblDetail.setItems(detailList);

        dpDari.setValue(LocalDate.now().withDayOfMonth(1));
        dpSampai.setValue(LocalDate.now());

        muatSemuaRiwayat();
    }

    // ── Setup Tabel ───────────────────────────────────────

    private void setupTabelRiwayat() {
        colNo.setCellValueFactory(cd -> {
            int idx = tblRiwayat.getItems().indexOf(cd.getValue()) + 1;
            return new SimpleStringProperty(String.valueOf(idx));
        });
        colTanggal.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getTanggal() != null
                        ? cd.getValue().getTanggal().format(TGL_FMT) : "-"));
        colWaktu.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getTanggal() != null
                        ? cd.getValue().getTanggal().format(WKT_FMT) : "-"));
        colId.setCellValueFactory(cd ->
                new SimpleStringProperty("TRX-" + String.format("%05d", cd.getValue().getIdTransaksi())));
        colMetode.setCellValueFactory(cd ->
                new SimpleStringProperty(
                        cd.getValue().getMetodeBayar() == Transaksi.MetodeBayar.cash
                                ? "TUNAI" : "CASHLESS"));
        colDiskon.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getDiskon() + "%"));
        colTotal.setCellValueFactory(cd ->
                new SimpleStringProperty(CurrencyUtil.format(cd.getValue().getTotalHarga())));

        colAksi.setCellFactory(col -> new TableCell<>() {
            private final Button btnDetail = new Button("Detail");
            {
                btnDetail.getStyleClass().add("btn-primary");
                btnDetail.setStyle("-fx-font-size:11px;");
                btnDetail.setOnAction(e -> {
                    Transaksi t = getTableView().getItems().get(getIndex());
                    tampilkanDetail(t);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnDetail);
            }
        });
    }

    private void setupTabelDetail() {
        colDetNo.setCellValueFactory(cd -> {
            int idx = tblDetail.getItems().indexOf(cd.getValue()) + 1;
            return new SimpleStringProperty(String.valueOf(idx));
        });
        colDetKode.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getIdBarang()));
        colDetNama.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getNamaBarang() != null
                        ? cd.getValue().getNamaBarang() : "-"));
        colDetHarga.setCellValueFactory(cd ->
                new SimpleStringProperty(CurrencyUtil.format(cd.getValue().getHargaSatuan())));
        colDetQty.setCellValueFactory(cd ->
                new SimpleStringProperty(String.valueOf(cd.getValue().getJumlah())));
        colDetDiskon.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getDiskonItem() + "%"));
        colDetSubtotal.setCellValueFactory(cd ->
                new SimpleStringProperty(CurrencyUtil.format(cd.getValue().getSubtotal())));
    }

    // ── Handlers ──────────────────────────────────────────

    @FXML
    private void handleFilter() {
        LocalDate dari   = dpDari.getValue();
        LocalDate sampai = dpSampai.getValue();
        if (dari == null || sampai == null) {
            AlertUtil.showWarning("Filter", "Pilih rentang tanggal.");
            return;
        }
        riwayatList.setAll(transaksiService.getRiwayatByPeriode(dari, sampai));
    }

    @FXML
    private void handleTampilSemua() {
        muatSemuaRiwayat();
    }

    @FXML
    private void handlePilihTransaksi() {
        Transaksi t = tblRiwayat.getSelectionModel().getSelectedItem();
        if (t != null) tampilkanDetail(t);
    }

    @FXML
    private void handleKembali() {
        viewDetail.setVisible(false);
        viewDaftar.setVisible(true);
        tblRiwayat.getSelectionModel().clearSelection();
    }

    // ── Helpers ───────────────────────────────────────────

    private void tampilkanDetail(Transaksi t) {
        // Muat detail lengkap dari database
        List<DetailTransaksi> items = transaksiService.getDetailByTransaksiId(t.getIdTransaksi());
        detailList.setAll(items);

        // Isi label header
        if (t.getTanggal() != null) {
            lblDetTanggal.setText(t.getTanggal().format(TGL_FMT));
            lblDetWaktu.setText(t.getTanggal().format(WKT_FMT));
        }
        lblDetId.setText("TRX-" + String.format("%05d", t.getIdTransaksi()));
        lblDetTotal.setText(CurrencyUtil.format(t.getTotalHarga()));
        lblDetTotalBawah.setText(CurrencyUtil.format(t.getTotalHarga()));

        // Switch view
        viewDaftar.setVisible(false);
        viewDetail.setVisible(true);
    }

    private void muatSemuaRiwayat() {
        riwayatList.setAll(transaksiService.getRiwayatSemua());
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
}
