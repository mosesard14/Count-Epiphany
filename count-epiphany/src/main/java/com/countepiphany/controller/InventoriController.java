package com.countepiphany.controller;

import com.countepiphany.MainApp;
import com.countepiphany.model.Barang;
import com.countepiphany.model.Supplier;
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

import java.util.List;
import java.util.stream.Collectors;

/**
 * InventoriController — Controller halaman Manajemen Inventori.
 */
public class InventoriController {

    @FXML private Label     lblFormTitle;
    @FXML private Button    btnHapus;

    // Form fields
    @FXML private TextField          txtIdBarang;
    @FXML private TextField          txtNamaBarang;
    @FXML private TextField          txtHargaBeli;
    @FXML private TextField          txtHargaJual;
    @FXML private TextField          txtKategori;
    @FXML private ComboBox<String>   cmbSupplier;
    @FXML private TextField          txtStokMin;

    // Filter
    @FXML private TextField          txtCariBarang;
    @FXML private ComboBox<String>   cmbFilterKategori;
    @FXML private ComboBox<String>   cmbFilterStok;

    // Table
    @FXML private TableView<Barang>                 tblBarang;
    @FXML private TableColumn<Barang, String>       colKode;
    @FXML private TableColumn<Barang, String>       colNama;
    @FXML private TableColumn<Barang, String>       colHargaBeli;
    @FXML private TableColumn<Barang, String>       colHargaJual;
    @FXML private TableColumn<Barang, String>       colStok;
    @FXML private TableColumn<Barang, String>       colKategori;
    @FXML private TableColumn<Barang, String>       colSupplier;
    @FXML private TableColumn<Barang, Void>         colAksi;

    @FXML private Label lblStokWarning;

    private final BarangService    barangService   = new BarangService();
    private final SupplierService  supplierService = new SupplierService();
    private final AuthService      authService     = new AuthService();

    private final ObservableList<Barang> barangList = FXCollections.observableArrayList();
    private boolean isEditMode = false;

    @FXML
    public void initialize() {
        setupTable();
        setupComboBoxes();
        tblBarang.setItems(barangList);
        btnHapus.setVisible(false);
        muatSemuaBarang();
        cekStokRendah();
    }

    // ── Setup ─────────────────────────────────────────────

    private void setupTable() {
        colKode.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getIdBarang()));
        colNama.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getNamaBarang()));
        colHargaBeli.setCellValueFactory(cd ->
                new SimpleStringProperty(CurrencyUtil.format(cd.getValue().getHargaBeli())));
        colHargaJual.setCellValueFactory(cd ->
                new SimpleStringProperty(CurrencyUtil.format(cd.getValue().getHargaJual())));
        colStok.setCellValueFactory(cd ->
                new SimpleStringProperty(String.valueOf(cd.getValue().getStok())));
        colKategori.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getKategori() == null ? "-" : cd.getValue().getKategori()));
        colSupplier.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getIdSupplier() == null ? "-" : cd.getValue().getIdSupplier()));

        // Row factory: warnai baris stok rendah
        tblBarang.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Barang item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else if (item.isStokRendah()) {
                    setStyle("-fx-background-color: #FFF3E0;");
                } else {
                    setStyle("");
                }
            }
        });

        // Kolom aksi
        colAksi.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit   = new Button("Edit");
            private final Button btnHapusTbl = new Button("Hapus");
            private final javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(6, btnEdit, btnHapusTbl);

            {
                btnEdit.getStyleClass().add("btn-primary");
                btnEdit.setStyle("-fx-font-size:11px;");
                btnHapusTbl.getStyleClass().add("btn-danger");
                btnHapusTbl.setStyle("-fx-font-size:11px;");

                btnEdit.setOnAction(e -> {
                    Barang b = getTableView().getItems().get(getIndex());
                    isiFormUntukEdit(b);
                });
                btnHapusTbl.setOnAction(e -> {
                    Barang b = getTableView().getItems().get(getIndex());
                    hapusBarang(b);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void setupComboBoxes() {
        // Supplier untuk form
        List<String> supplierIds = supplierService.getAllSupplier().stream()
                .map(s -> s.getIdSupplier() + " - " + s.getNamaSupplier())
                .collect(Collectors.toList());
        cmbSupplier.setItems(FXCollections.observableArrayList(supplierIds));

        // Filter kategori
        List<String> kategoriList = barangService.getAllKategori();
        kategoriList.add(0, "Semua");
        cmbFilterKategori.setItems(FXCollections.observableArrayList(kategoriList));
        cmbFilterKategori.setValue("Semua");

        // Filter stok
        cmbFilterStok.setItems(FXCollections.observableArrayList("Semua", "Habis", "Rendah"));
        cmbFilterStok.setValue("Semua");
    }

    // ── CRUD Handlers ─────────────────────────────────────

    @FXML
    private void handleSimpan() {
        try {
            String idBarang    = txtIdBarang.getText().trim();
            String namaBarang  = txtNamaBarang.getText().trim();
            double hargaBeli   = Double.parseDouble(txtHargaBeli.getText().trim().replace(",", "."));
            double hargaJual   = Double.parseDouble(txtHargaJual.getText().trim().replace(",", "."));
            String kategori    = txtKategori.getText().trim();
            int stokMin        = Integer.parseInt(txtStokMin.getText().trim().isEmpty() ? "5" : txtStokMin.getText().trim());

            String idSupplier = null;
            if (cmbSupplier.getValue() != null) {
                idSupplier = cmbSupplier.getValue().split(" - ")[0];
            }

            if (isEditMode) {
                Barang b = new Barang(idBarang, namaBarang, hargaBeli, hargaJual,
                        0, kategori, idSupplier, stokMin);
                barangService.updateBarang(b);
                AlertUtil.showInfo("Berhasil", "Data barang berhasil diperbarui.");
            } else {
                barangService.tambahBarang(idBarang, namaBarang, hargaBeli, hargaJual,
                        kategori, idSupplier, stokMin);
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
        if (AlertUtil.showConfirmation("Hapus Barang",
                "Yakin ingin menghapus barang dengan kode '" + id + "'?")) {
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
        if (AlertUtil.showConfirmation("Hapus Barang",
                "Hapus barang '" + b.getNamaBarang() + "'?")) {
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

    // ── Filter & Search ───────────────────────────────────

    @FXML
    private void handleCari() {
        handleFilter();
    }

    @FXML
    private void handleFilter() {
        String keyword    = txtCariBarang.getText().trim();
        String kategori   = cmbFilterKategori.getValue();
        String filterStok = cmbFilterStok.getValue();

        List<Barang> hasil = barangService.filterBarang(keyword, kategori, filterStok);
        barangList.setAll(hasil);
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
        Barang selected = tblBarang.getSelectionModel().getSelectedItem();
        if (selected != null) isiFormUntukEdit(selected);
    }

    // ── Helpers ───────────────────────────────────────────

    private void isiFormUntukEdit(Barang b) {
        isEditMode = true;
        lblFormTitle.setText("Update Barang  (Update Stok/Harga)");
        btnHapus.setVisible(true);
        txtIdBarang.setText(b.getIdBarang());
        txtIdBarang.setEditable(false);
        txtNamaBarang.setText(b.getNamaBarang());
        txtHargaBeli.setText(String.valueOf(b.getHargaBeli()));
        txtHargaJual.setText(String.valueOf(b.getHargaJual()));
        txtKategori.setText(b.getKategori() == null ? "" : b.getKategori());
        txtStokMin.setText(String.valueOf(b.getStokMinimum()));
        if (b.getIdSupplier() != null) {
            cmbSupplier.getItems().stream()
                    .filter(s -> s.startsWith(b.getIdSupplier()))
                    .findFirst()
                    .ifPresent(s -> cmbSupplier.setValue(s));
        }
    }

    private void muatSemuaBarang() {
        barangList.setAll(barangService.getAllBarang());
    }

    private void cekStokRendah() {
        List<Barang> rendah = barangService.getBarangStokRendah();
        if (rendah.isEmpty()) {
            lblStokWarning.setText("");
        } else {
            String nama = rendah.stream()
                    .map(Barang::getNamaBarang)
                    .limit(5)
                    .collect(Collectors.joining(", "));
            lblStokWarning.setText("⚠ Stok rendah: " + nama
                    + (rendah.size() > 5 ? " dan " + (rendah.size() - 5) + " lainnya." : "."));
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
}
