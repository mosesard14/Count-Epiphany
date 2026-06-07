package com.countepiphany.controller;

import com.countepiphany.MainApp;
import com.countepiphany.model.Barang;
import com.countepiphany.service.AuthService;
import com.countepiphany.service.BarangService;
import com.countepiphany.service.SupplierService;
import com.countepiphany.util.AlertUtil;
import com.countepiphany.util.CurrencyUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.stream.Collectors;

public class InventoriController {

    // ── Form fields ───────────────────────────────────────
    @FXML private Label            lblFormTitle;
    @FXML private Button           btnHapus;
    @FXML private TextField        txtIdBarang;
    @FXML private TextField        txtNamaBarang;
    @FXML private TextField        txtHargaBeli;
    @FXML private TextField        txtHargaJual;
    @FXML private TextField        txtKategori;
    @FXML private ComboBox<String> cmbSupplier;
    @FXML private TextField        txtStokMin;

    // ── Filter ────────────────────────────────────────────
    @FXML private TextField        txtCariBarang;
    @FXML private ComboBox<String> cmbFilterKategori;
    @FXML private ComboBox<String> cmbFilterStok;

    // ── Tabel utama ───────────────────────────────────────
    @FXML private TableView<Barang>           tblBarang;
    @FXML private TableColumn<Barang, String> colKode;
    @FXML private TableColumn<Barang, String> colNama;
    @FXML private TableColumn<Barang, String> colHargaBeli;
    @FXML private TableColumn<Barang, String> colHargaJual;
    @FXML private TableColumn<Barang, String> colStok;
    @FXML private TableColumn<Barang, String> colKategori;
    @FXML private TableColumn<Barang, String> colSupplier;
    @FXML private TableColumn<Barang, Void>   colAksi;

    @FXML private Label lblStokWarning;

    private final ObservableList<Barang> listSemua = FXCollections.observableArrayList();

    private final BarangService   barangService   = new BarangService();
    private final SupplierService supplierService = new SupplierService();
    private final AuthService     authService     = new AuthService();

    private boolean isEditMode = false;

    // ── Init ──────────────────────────────────────────────

    @FXML
    public void initialize() {
        setupTabelUtama();
        tblBarang.setItems(listSemua);
        setupComboBoxes();
        muatSemuaBarang();
        cekStokRendah();
    }

    // ── Setup tabel ───────────────────────────────────────

    private void setupTabelUtama() {
        colKode.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getIdBarang()));
        colNama.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getNamaBarang()));
        colHargaBeli.setCellValueFactory(cd ->
                new SimpleStringProperty(CurrencyUtil.format(cd.getValue().getHargaBeli())));
        colHargaJual.setCellValueFactory(cd ->
                new SimpleStringProperty(CurrencyUtil.format(cd.getValue().getHargaJual())));
        colStok.setCellValueFactory(cd ->
                new SimpleStringProperty(String.valueOf(cd.getValue().getStok())));
        colKategori.setCellValueFactory(cd ->
                new SimpleStringProperty(nvl(cd.getValue().getKategori())));
        colSupplier.setCellValueFactory(cd ->
                new SimpleStringProperty(nvl(cd.getValue().getIdSupplier())));
        colAksi.setCellFactory(col -> buatCellAksi());

        tblBarang.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(Barang b, boolean empty) {
                super.updateItem(b, empty);
                setStyle(b != null && !empty && b.isStokRendah()
                        ? "-fx-background-color:#FFF3E0;" : "");
            }
        });
    }

    private TableCell<Barang, Void> buatCellAksi() {
        return new TableCell<>() {
            final Button btnEdit = new Button("Edit");
            final Button btnHps  = new Button("Hapus");
            final javafx.scene.layout.HBox box =
                    new javafx.scene.layout.HBox(6, btnEdit, btnHps);
            {
                btnEdit.getStyleClass().add("btn-primary");
                btnEdit.setStyle("-fx-font-size:11px;");
                btnHps.getStyleClass().add("btn-danger");
                btnHps.setStyle("-fx-font-size:11px;");
                btnEdit.setOnAction(e -> {
                    Barang b = tblBarang.getItems().get(getIndex());
                    if (b != null) isiFormUntukEdit(b);
                });
                btnHps.setOnAction(e -> {
                    Barang b = tblBarang.getItems().get(getIndex());
                    if (b != null) hapusBarang(b);
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        };
    }

    private void setupComboBoxes() {
        List<String> suppliers = supplierService.getAllSupplier().stream()
                .map(s -> s.getIdSupplier() + " - " + s.getNamaSupplier())
                .collect(Collectors.toList());
        cmbSupplier.setItems(FXCollections.observableArrayList(suppliers));

        cmbFilterKategori.setItems(FXCollections.observableArrayList(
                "Semua", "Makanan", "Minuman", "Snack", "Sembako", "Kebersihan", "Lainnya"));
        cmbFilterKategori.setValue("Semua");

        cmbFilterStok.setItems(FXCollections.observableArrayList("Semua", "Habis", "Rendah"));
        cmbFilterStok.setValue("Semua");
    }

    // ── CRUD handlers ─────────────────────────────────────

    @FXML
    private void handleSimpan() {
        try {
            String idBarang   = txtIdBarang.getText().trim();
            String namaBarang = txtNamaBarang.getText().trim();
            double hargaBeli  = Double.parseDouble(txtHargaBeli.getText().trim().replace(",", "."));
            double hargaJual  = Double.parseDouble(txtHargaJual.getText().trim().replace(",", "."));
            String kategori   = txtKategori.getText().trim();
            int stokMin       = txtStokMin.getText().trim().isEmpty() ? 5
                    : Integer.parseInt(txtStokMin.getText().trim());
            String idSupplier = null;
            if (cmbSupplier.getValue() != null)
                idSupplier = cmbSupplier.getValue().split(" - ")[0];

            if (isEditMode) {
                Barang b = new Barang(idBarang, namaBarang, hargaBeli, hargaJual,
                        0, kategori, idSupplier, stokMin);
                barangService.updateBarang(b);
                AlertUtil.showInfo("Berhasil", "Data barang berhasil diperbarui.");
            } else {
                barangService.tambahBarang(idBarang, namaBarang, hargaBeli, hargaJual,
                        kategori, null, idSupplier, stokMin);
                AlertUtil.showInfo("Berhasil", "Barang '" + namaBarang + "' berhasil ditambahkan.");
            }
            handleBatalForm();
            muatSemuaBarang();
            cekStokRendah();

        } catch (NumberFormatException e) {
            AlertUtil.showWarning("Input Salah", "Harga dan stok minimum harus berupa angka.");
        } catch (IllegalArgumentException e) {
            AlertUtil.showWarning("Validasi Gagal", e.getMessage());
        } catch (Exception e) {
            AlertUtil.showError("Error", e.getMessage());
        }
    }

    @FXML
    private void handleHapus() {
        String id = txtIdBarang.getText().trim();
        if (id.isEmpty()) return;
        if (AlertUtil.showConfirmation("Hapus Barang", "Yakin hapus barang '" + id + "'?")) {
            try {
                barangService.hapusBarang(id);
                AlertUtil.showInfo("Berhasil", "Barang berhasil dihapus.");
                handleBatalForm();
                muatSemuaBarang();
            } catch (Exception e) {
                AlertUtil.showError("Gagal Hapus", e.getMessage());
            }
        }
    }

    private void hapusBarang(Barang b) {
        if (AlertUtil.showConfirmation("Hapus Barang", "Hapus '" + b.getNamaBarang() + "'?")) {
            try {
                barangService.hapusBarang(b.getIdBarang());
                muatSemuaBarang();
            } catch (Exception e) {
                AlertUtil.showError("Gagal Hapus", e.getMessage());
            }
        }
    }

    @FXML
    private void handleBatalForm() {
        isEditMode = false;
        lblFormTitle.setText("Tambah Barang Baru");
        btnHapus.setVisible(false);
        txtIdBarang.clear();
        txtIdBarang.setEditable(true);
        txtNamaBarang.clear();
        txtHargaBeli.clear();
        txtHargaJual.clear();
        txtKategori.clear();
        txtStokMin.clear();
        cmbSupplier.setValue(null);
    }

    // ── Filter & search ───────────────────────────────────

    @FXML private void handleCari() { handleFilter(); }

    @FXML
    private void handleFilter() {
        String keyword    = txtCariBarang.getText().trim();
        String filterKat  = cmbFilterKategori.getValue();
        String filterStok = cmbFilterStok.getValue();

        List<Barang> hasil = barangService.filterBarang(keyword, filterKat, filterStok);
        listSemua.setAll(hasil);
    }

    @FXML
    private void handleReset() {
        txtCariBarang.clear();
        cmbFilterKategori.setValue("Semua");
        cmbFilterStok.setValue("Semua");
        muatSemuaBarang();
    }

    @FXML
    private void handlePilihBaris() {
        Barang sel = tblBarang.getSelectionModel().getSelectedItem();
        if (sel != null) isiFormUntukEdit(sel);
    }

    // ── Helpers ───────────────────────────────────────────

    private void isiFormUntukEdit(Barang b) {
        isEditMode = true;
        lblFormTitle.setText("Update Barang");
        btnHapus.setVisible(true);
        txtIdBarang.setText(b.getIdBarang());
        txtIdBarang.setEditable(false);
        txtNamaBarang.setText(b.getNamaBarang());
        txtHargaBeli.setText(String.valueOf(b.getHargaBeli()));
        txtHargaJual.setText(String.valueOf(b.getHargaJual()));
        txtKategori.setText(nvl(b.getKategori()));
        txtStokMin.setText(String.valueOf(b.getStokMinimum()));
        if (b.getIdSupplier() != null) {
            cmbSupplier.getItems().stream()
                    .filter(s -> s.startsWith(b.getIdSupplier()))
                    .findFirst().ifPresent(cmbSupplier::setValue);
        }
    }

    private void muatSemuaBarang() {
        listSemua.setAll(barangService.getAllBarang());
    }

    private void cekStokRendah() {
        List<Barang> rendah = barangService.getBarangStokRendah();
        if (rendah.isEmpty()) {
            lblStokWarning.setText("");
        } else {
            String nama = rendah.stream().map(Barang::getNamaBarang)
                    .limit(5).collect(Collectors.joining(", "));
            lblStokWarning.setText("Stok rendah: " + nama
                    + (rendah.size() > 5 ? " dan " + (rendah.size() - 5) + " lainnya." : "."));
        }
    }

    private String nvl(String s) { return s != null ? s : "-"; }

    // ── Navigation ────────────────────────────────────────

    @FXML private void gotoTransaksi() { MainApp.navigateTo("transaksi.fxml"); }
    @FXML private void gotoInventori() { MainApp.navigateTo("inventori.fxml"); }
    @FXML private void gotoSupplier()  { MainApp.navigateTo("supplier.fxml");  }
    @FXML private void gotoLaporan()   { MainApp.navigateTo("laporan.fxml");   }
    @FXML private void gotoRiwayat()   { MainApp.navigateTo("riwayat.fxml");   }
    @FXML private void gotoPembelian() { MainApp.navigateTo("pembelian.fxml"); }

    @FXML
    private void handleLogout() {
        if (AlertUtil.showConfirmation("Logout", "Yakin ingin keluar?")) {
            authService.logout();
            MainApp.navigateTo("login.fxml");
        }
    }
}