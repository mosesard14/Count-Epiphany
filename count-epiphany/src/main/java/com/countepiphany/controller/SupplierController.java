package com.countepiphany.controller;

import com.countepiphany.MainApp;
import com.countepiphany.model.Supplier;
import com.countepiphany.service.AuthService;
import com.countepiphany.service.SupplierService;
import com.countepiphany.util.AlertUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * SupplierController — Controller halaman Manajemen Supplier.
 */
public class SupplierController {

    @FXML private Label  lblFormTitle;
    @FXML private Button btnHapus;

    @FXML private TextField txtIdSupplier;
    @FXML private TextField txtNamaSupplier;
    @FXML private TextField txtAlamat;
    @FXML private TextField txtTelepon;
    @FXML private TextField txtEmail;

    @FXML private TextField txtCariSupplier;

    @FXML private TableView<Supplier>                 tblSupplier;
    @FXML private TableColumn<Supplier, String>       colId;
    @FXML private TableColumn<Supplier, String>       colNama;
    @FXML private TableColumn<Supplier, String>       colAlamat;
    @FXML private TableColumn<Supplier, String>       colTelepon;
    @FXML private TableColumn<Supplier, String>       colEmail;
    @FXML private TableColumn<Supplier, Void>         colAksi;

    private final SupplierService supplierService = new SupplierService();
    private final AuthService     authService     = new AuthService();

    private final ObservableList<Supplier> supplierList = FXCollections.observableArrayList();
    private boolean isEditMode = false;

    @FXML
    public void initialize() {
        setupTable();
        tblSupplier.setItems(supplierList);
        muatSemuaSupplier();
    }

    private void setupTable() {
        colId.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getIdSupplier()));
        colNama.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getNamaSupplier()));
        colAlamat.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getAlamat() == null ? "-" : cd.getValue().getAlamat()));
        colTelepon.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getTelepon() == null ? "-" : cd.getValue().getTelepon()));
        colEmail.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getEmail() == null ? "-" : cd.getValue().getEmail()));

        colAksi.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit  = new Button("Edit");
            private final Button btnDel   = new Button("Hapus");
            private final javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(6, btnEdit, btnDel);

            {
                btnEdit.getStyleClass().add("btn-primary");
                btnEdit.setStyle("-fx-font-size:11px;");
                btnDel.getStyleClass().add("btn-danger");
                btnDel.setStyle("-fx-font-size:11px;");

                btnEdit.setOnAction(e -> isiFormUntukEdit(getTableView().getItems().get(getIndex())));
                btnDel.setOnAction(e  -> hapusSupplier(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    @FXML
    private void handleSimpan() {
        try {
            String id    = txtIdSupplier.getText().trim();
            String nama  = txtNamaSupplier.getText().trim();
            String alamat  = txtAlamat.getText().trim();
            String telepon = txtTelepon.getText().trim();
            String email   = txtEmail.getText().trim();

            if (isEditMode) {
                Supplier s = new Supplier(id, nama, alamat, telepon, email);
                supplierService.updateSupplier(s);
                AlertUtil.showInfo("Berhasil", "Data supplier berhasil diperbarui.");
            } else {
                supplierService.tambahSupplier(id, nama, alamat, telepon, email);
                AlertUtil.showInfo("Berhasil", "Supplier '" + nama + "' berhasil ditambahkan.");
            }
            handleBatalForm();
            muatSemuaSupplier();
        } catch (IllegalArgumentException e) {
            AlertUtil.showWarning("Validasi Gagal", e.getMessage());
        } catch (Exception e) {
            AlertUtil.showError("Error", e.getMessage());
        }
    }

    @FXML
    private void handleHapus() {
        String id = txtIdSupplier.getText().trim();
        if (id.isEmpty()) return;
        if (AlertUtil.showConfirmation("Hapus Supplier",
                "Yakin ingin menghapus supplier dengan ID '" + id + "'?\n"
                + "Pastikan tidak ada barang yang terhubung ke supplier ini.")) {
            try {
                supplierService.hapusSupplier(id);
                AlertUtil.showInfo("Berhasil", "Supplier berhasil dihapus.");
                handleBatalForm();
                muatSemuaSupplier();
            } catch (Exception e) {
                AlertUtil.showError("Gagal Hapus", e.getMessage());
            }
        }
    }

    private void hapusSupplier(Supplier s) {
        if (AlertUtil.showConfirmation("Hapus Supplier",
                "Hapus supplier '" + s.getNamaSupplier() + "'?")) {
            try {
                supplierService.hapusSupplier(s.getIdSupplier());
                muatSemuaSupplier();
            } catch (Exception e) {
                AlertUtil.showError("Gagal Hapus", e.getMessage());
            }
        }
    }

    @FXML
    private void handleBatalForm() {
        isEditMode = false;
        lblFormTitle.setText("Tambah Supplier");
        btnHapus.setVisible(false);
        txtIdSupplier.clear();
        txtIdSupplier.setEditable(true);
        txtNamaSupplier.clear();
        txtAlamat.clear();
        txtTelepon.clear();
        txtEmail.clear();
    }

    @FXML
    private void handleCari() {
        String keyword = txtCariSupplier.getText().trim();
        supplierList.setAll(supplierService.cariByNama(keyword));
    }

    @FXML
    private void handleReset() {
        txtCariSupplier.clear();
        muatSemuaSupplier();
    }

    @FXML
    private void handlePilihBaris() {
        Supplier s = tblSupplier.getSelectionModel().getSelectedItem();
        if (s != null) isiFormUntukEdit(s);
    }

    private void isiFormUntukEdit(Supplier s) {
        isEditMode = true;
        lblFormTitle.setText("Update Supplier");
        btnHapus.setVisible(true);
        txtIdSupplier.setText(s.getIdSupplier());
        txtIdSupplier.setEditable(false);
        txtNamaSupplier.setText(s.getNamaSupplier());
        txtAlamat.setText(s.getAlamat() == null ? "" : s.getAlamat());
        txtTelepon.setText(s.getTelepon() == null ? "" : s.getTelepon());
        txtEmail.setText(s.getEmail() == null ? "" : s.getEmail());
    }

    private void muatSemuaSupplier() {
        supplierList.setAll(supplierService.getAllSupplier());
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
