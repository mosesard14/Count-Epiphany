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

/**
 * InventoriController — halaman manajemen inventori.
 * Fitur: Tab kategori (induk) + panel subkategori dinamis (jenis).
 *
 * Alur subkategori:
 *   1. User klik tab kategori (Minuman, Sembako, dll)
 *   2. handleTabKategoriChanged() dipanggil
 *   3. Panel kiri diisi tombol subkategori dari DB (Susu, Kopi, Air Mineral, dll)
 *   4. User klik salah satu tombol subkategori
 *   5. Tabel difilter hanya tampilkan barang dengan subkategori itu
 */
public class InventoriController {

    // ── Form fields ───────────────────────────────────────
    @FXML private Label               lblFormTitle;
    @FXML private Button              btnHapus;
    @FXML private TextField           txtIdBarang;
    @FXML private TextField           txtNamaBarang;
    @FXML private TextField           txtHargaBeli;
    @FXML private TextField           txtHargaJual;
    @FXML private TextField           txtKategori;
    @FXML private TextField           txtSubkategori;
    @FXML private ComboBox<String>    cmbSupplier;
    @FXML private TextField           txtStokMin;

    // ── Filter ────────────────────────────────────────────
    @FXML private TextField           txtCariBarang;
    @FXML private ComboBox<String>    cmbFilterStok;

    // ── Panel subkategori (kiri) ──────────────────────────
    @FXML private VBox                panelSubkategori;
    @FXML private Button              btnSubSemua;
    @FXML private VBox                vboxSubkategoriBtns;

    // ── Tab pane ──────────────────────────────────────────
    @FXML private TabPane             tabPaneKategori;
    @FXML private Tab                 tabSemua;
    @FXML private Tab                 tabMakanan;
    @FXML private Tab                 tabMinuman;
    @FXML private Tab                 tabSnack;
    @FXML private Tab                 tabSembako;
    @FXML private Tab                 tabKebersihan;
    @FXML private Tab                 tabLainnya;

    // ── Tabel per tab ─────────────────────────────────────
    @FXML private TableView<Barang>             tblBarang;
    @FXML private TableColumn<Barang, String>   colKode;
    @FXML private TableColumn<Barang, String>   colNama;
    @FXML private TableColumn<Barang, String>   colHargaBeli;
    @FXML private TableColumn<Barang, String>   colHargaJual;
    @FXML private TableColumn<Barang, String>   colStok;
    @FXML private TableColumn<Barang, String>   colKategori;
    @FXML private TableColumn<Barang, String>   colSubkategori;
    @FXML private TableColumn<Barang, String>   colSupplier;
    @FXML private TableColumn<Barang, Void>     colAksi;

    @FXML private TableView<Barang> tblMakanan;
    @FXML private TableView<Barang> tblMinuman;
    @FXML private TableView<Barang> tblSnack;
    @FXML private TableView<Barang> tblSembako;
    @FXML private TableView<Barang> tblKebersihan;
    @FXML private TableView<Barang> tblLainnya;

    @FXML private Label lblStokWarning;

    // ── Data lists ────────────────────────────────────────
    private final ObservableList<Barang> listSemua      = FXCollections.observableArrayList();
    private final ObservableList<Barang> listMakanan    = FXCollections.observableArrayList();
    private final ObservableList<Barang> listMinuman    = FXCollections.observableArrayList();
    private final ObservableList<Barang> listSnack      = FXCollections.observableArrayList();
    private final ObservableList<Barang> listSembako    = FXCollections.observableArrayList();
    private final ObservableList<Barang> listKebersihan = FXCollections.observableArrayList();
    private final ObservableList<Barang> listLainnya    = FXCollections.observableArrayList();

    private static final String[] KATEGORI_TETAP =
        {"Makanan", "Minuman", "Snack", "Sembako", "Kebersihan"};

    private final BarangService   barangService   = new BarangService();
    private final SupplierService supplierService = new SupplierService();
    private final AuthService     authService     = new AuthService();

    // State
    private boolean isEditMode           = false;
    private String  subkategoriAktif     = null; // null = semua jenis

    // ── Init ──────────────────────────────────────────────

    @FXML
    public void initialize() {
        setupTabelUtama();
        setupTabelKategori(tblMakanan,    listMakanan);
        setupTabelKategori(tblMinuman,    listMinuman);
        setupTabelKategori(tblSnack,      listSnack);
        setupTabelKategori(tblSembako,    listSembako);
        setupTabelKategori(tblKebersihan, listKebersihan);
        setupTabelKategori(tblLainnya,    listLainnya);

        tblBarang.setItems(listSemua);
        setupComboBoxes();
        muatSemuaBarang();
        cekStokRendah();
        muatSubkategoriPanel("Semua");
    }

    // ── Setup tabel utama ─────────────────────────────────

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
        colSubkategori.setCellValueFactory(cd ->
            new SimpleStringProperty(nvl(cd.getValue().getSubkategori())));
        colSupplier.setCellValueFactory(cd ->
            new SimpleStringProperty(nvl(cd.getValue().getIdSupplier())));

        colAksi.setCellFactory(col -> buatCellAksi(tblBarang));

        tblBarang.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(Barang b, boolean empty) {
                super.updateItem(b, empty);
                setStyle(b != null && !empty && b.isStokRendah()
                    ? "-fx-background-color:#FFF3E0;" : "");
            }
        });
    }

    // ── Setup tabel per kategori ──────────────────────────

    @SuppressWarnings("unchecked")
    private void setupTabelKategori(TableView<Barang> tbl, ObservableList<Barang> list) {
        tbl.setItems(list);
        var cols = tbl.getColumns();
        if (cols.size() < 8) return;

        // Urutan kolom di FXML: Kode, Nama, HargaBeli, HargaJual, Stok, Jenis, Supplier, Aksi
        ((TableColumn<Barang,String>) cols.get(0)).setCellValueFactory(cd ->
            new SimpleStringProperty(cd.getValue().getIdBarang()));
        ((TableColumn<Barang,String>) cols.get(1)).setCellValueFactory(cd ->
            new SimpleStringProperty(cd.getValue().getNamaBarang()));
        ((TableColumn<Barang,String>) cols.get(2)).setCellValueFactory(cd ->
            new SimpleStringProperty(CurrencyUtil.format(cd.getValue().getHargaBeli())));
        ((TableColumn<Barang,String>) cols.get(3)).setCellValueFactory(cd ->
            new SimpleStringProperty(CurrencyUtil.format(cd.getValue().getHargaJual())));
        ((TableColumn<Barang,String>) cols.get(4)).setCellValueFactory(cd ->
            new SimpleStringProperty(String.valueOf(cd.getValue().getStok())));
        ((TableColumn<Barang,String>) cols.get(5)).setCellValueFactory(cd ->
            new SimpleStringProperty(nvl(cd.getValue().getSubkategori())));
        ((TableColumn<Barang,String>) cols.get(6)).setCellValueFactory(cd ->
            new SimpleStringProperty(nvl(cd.getValue().getIdSupplier())));

        ((TableColumn<Barang,Void>) cols.get(7)).setCellFactory(col -> buatCellAksi(tbl));

        tbl.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(Barang b, boolean empty) {
                super.updateItem(b, empty);
                setStyle(b != null && !empty && b.isStokRendah()
                    ? "-fx-background-color:#FFF3E0;" : "");
            }
        });

        tbl.setOnMouseClicked(e -> {
            Barang sel = tbl.getSelectionModel().getSelectedItem();
            if (sel != null) isiFormUntukEdit(sel);
        });
    }

    private TableCell<Barang, Void> buatCellAksi(TableView<Barang> tbl) {
        return new TableCell<>() {
            final Button btnEdit  = new Button("Edit");
            final Button btnHps   = new Button("Hapus");
            final javafx.scene.layout.HBox box =
                new javafx.scene.layout.HBox(6, btnEdit, btnHps);
            {
                btnEdit.getStyleClass().add("btn-primary");
                btnEdit.setStyle("-fx-font-size:11px;");
                btnHps.getStyleClass().add("btn-danger");
                btnHps.setStyle("-fx-font-size:11px;");
                btnEdit.setOnAction(e -> {
                    Barang b = tbl.getItems().get(getIndex());
                    if (b != null) isiFormUntukEdit(b);
                });
                btnHps.setOnAction(e -> {
                    Barang b = tbl.getItems().get(getIndex());
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
        List<String> ids = supplierService.getAllSupplier().stream()
            .map(s -> s.getIdSupplier() + " - " + s.getNamaSupplier())
            .collect(Collectors.toList());
        cmbSupplier.setItems(FXCollections.observableArrayList(ids));
        cmbFilterStok.setItems(FXCollections.observableArrayList("Semua", "Habis", "Rendah"));
        cmbFilterStok.setValue("Semua");
    }

    // ── Panel subkategori dinamis ─────────────────────────

    /**
     * Isi panel kiri dengan tombol subkategori dari kategori yang aktif.
     * kategoriAktif = "Semua" artinya tampilkan semua subkategori dari semua kategori.
     */
    private void muatSubkategoriPanel(String kategoriAktif) {
        vboxSubkategoriBtns.getChildren().clear();
        subkategoriAktif = null;

        // Reset style btnSubSemua
        btnSubSemua.setStyle("-fx-font-size:12px;-fx-background-color:#6C63CF;" +
                             "-fx-text-fill:white;-fx-cursor:hand;-fx-padding:8 10;");

        List<String> subs;
        if ("Semua".equals(kategoriAktif)) {
            // Ambil semua subkategori unik dari semua kategori
            subs = barangService.getAllBarang().stream()
                .map(Barang::getSubkategori)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        } else {
            subs = barangService.getSubkategoriByKategori(kategoriAktif);
        }

        for (String sub : subs) {
            Button btn = new Button(sub);
            btn.setPrefWidth(155);
            btn.setStyle("-fx-font-size:12px;-fx-background-color:transparent;" +
                         "-fx-text-fill:#4A4A6A;-fx-cursor:hand;" +
                         "-fx-padding:8 10;-fx-alignment:CENTER_LEFT;");
            btn.setOnAction(e -> handlePilihSubkategori(sub, btn));
            vboxSubkategoriBtns.getChildren().add(btn);
        }
    }

    // ── Tab handler ───────────────────────────────────────

    @FXML
    private void handleTabKategoriChanged() {
        subkategoriAktif = null;
        Tab aktif = tabPaneKategori.getSelectionModel().getSelectedItem();
        if (aktif == null) return;

        String judulTab = aktif.getText();
        muatSubkategoriPanel(judulTab);
        handleFilter();
    }

    // ── Subkategori handler ───────────────────────────────

    @FXML
    private void handleSubkategoriSemua() {
        subkategoriAktif = null;
        // Highlight tombol Semua, reset tombol lain
        btnSubSemua.setStyle("-fx-font-size:12px;-fx-background-color:#6C63CF;" +
                             "-fx-text-fill:white;-fx-cursor:hand;-fx-padding:8 10;");
        vboxSubkategoriBtns.getChildren().forEach(node -> {
            if (node instanceof Button b) {
                b.setStyle("-fx-font-size:12px;-fx-background-color:transparent;" +
                           "-fx-text-fill:#4A4A6A;-fx-cursor:hand;" +
                           "-fx-padding:8 10;-fx-alignment:CENTER_LEFT;");
            }
        });
        handleFilter();
    }

    private void handlePilihSubkategori(String sub, Button btnDipilih) {
        subkategoriAktif = sub;

        // Reset semua tombol
        btnSubSemua.setStyle("-fx-font-size:12px;-fx-background-color:transparent;" +
                             "-fx-text-fill:#4A4A6A;-fx-cursor:hand;-fx-padding:8 10;");
        vboxSubkategoriBtns.getChildren().forEach(node -> {
            if (node instanceof Button b) {
                b.setStyle("-fx-font-size:12px;-fx-background-color:transparent;" +
                           "-fx-text-fill:#4A4A6A;-fx-cursor:hand;" +
                           "-fx-padding:8 10;-fx-alignment:CENTER_LEFT;");
            }
        });

        // Highlight tombol yang dipilih
        btnDipilih.setStyle("-fx-font-size:12px;-fx-background-color:#6C63CF;" +
                            "-fx-text-fill:white;-fx-cursor:hand;" +
                            "-fx-padding:8 10;-fx-alignment:CENTER_LEFT;");

        handleFilter();
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
            String subkat     = txtSubkategori.getText().trim();
            int stokMin       = txtStokMin.getText().trim().isEmpty() ? 5
                                : Integer.parseInt(txtStokMin.getText().trim());
            String idSupplier = null;
            if (cmbSupplier.getValue() != null)
                idSupplier = cmbSupplier.getValue().split(" - ")[0];

            if (isEditMode) {
                Barang b = new Barang(idBarang, namaBarang, hargaBeli, hargaJual,
                                      0, kategori, subkat, idSupplier, stokMin);
                barangService.updateBarang(b);
                AlertUtil.showInfo("Berhasil", "Data barang berhasil diperbarui.");
            } else {
                barangService.tambahBarang(idBarang, namaBarang, hargaBeli, hargaJual,
                                           kategori, subkat, idSupplier, stokMin);
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
                "Yakin hapus barang '" + id + "'?")) {
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
                "Hapus '" + b.getNamaBarang() + "'?")) {
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
        txtSubkategori.clear();
        txtStokMin.clear();
        cmbSupplier.setValue(null);
    }

    // ── Filter & search ───────────────────────────────────

    @FXML private void handleCari()   { handleFilter(); }

    @FXML
    private void handleFilter() {
        String keyword   = txtCariBarang.getText().trim();
        String filterStk = cmbFilterStok.getValue();
        Tab aktif        = tabPaneKategori.getSelectionModel().getSelectedItem();
        String judulTab  = aktif != null ? aktif.getText() : "Semua";

        // Ambil data berdasarkan keyword dan filter stok
        List<Barang> base = barangService.filterBarang(keyword, "Semua", filterStk);

        // Filter subkategori jika ada yang dipilih
        if (subkategoriAktif != null) {
            base = base.stream()
                .filter(b -> subkategoriAktif.equalsIgnoreCase(b.getSubkategori()))
                .collect(Collectors.toList());
        }

        // Distribusi ke tiap list
        final List<Barang> data = base;
        listSemua.setAll(data);
        listMakanan.setAll(filterKat(data, "Makanan"));
        listMinuman.setAll(filterKat(data, "Minuman"));
        listSnack.setAll(filterKat(data, "Snack"));
        listSembako.setAll(filterKat(data, "Sembako"));
        listKebersihan.setAll(filterKat(data, "Kebersihan"));
        listLainnya.setAll(data.stream()
            .filter(b -> {
                String k = b.getKategori();
                if (k == null || k.isBlank()) return true;
                for (String kp : KATEGORI_TETAP) if (kp.equalsIgnoreCase(k)) return false;
                return true;
            }).collect(Collectors.toList()));
    }

    @FXML
    private void handleReset() {
        txtCariBarang.clear();
        cmbFilterStok.setValue("Semua");
        subkategoriAktif = null;
        muatSemuaBarang();
        muatSubkategoriPanel("Semua");
    }

    @FXML
    private void handlePilihBaris() {
        Barang sel = tblBarang.getSelectionModel().getSelectedItem();
        if (sel != null) isiFormUntukEdit(sel);
    }

    // ── Helpers ───────────────────────────────────────────

    private List<Barang> filterKat(List<Barang> src, String kat) {
        return src.stream()
            .filter(b -> kat.equalsIgnoreCase(b.getKategori()))
            .collect(Collectors.toList());
    }

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
        txtSubkategori.setText(nvl(b.getSubkategori()));
        txtStokMin.setText(String.valueOf(b.getStokMinimum()));
        if (b.getIdSupplier() != null) {
            cmbSupplier.getItems().stream()
                .filter(s -> s.startsWith(b.getIdSupplier()))
                .findFirst().ifPresent(s -> cmbSupplier.setValue(s));
        }
    }

    private void muatSemuaBarang() {
        List<Barang> semua = barangService.getAllBarang();
        listSemua.setAll(semua);
        listMakanan.setAll(filterKat(semua, "Makanan"));
        listMinuman.setAll(filterKat(semua, "Minuman"));
        listSnack.setAll(filterKat(semua, "Snack"));
        listSembako.setAll(filterKat(semua, "Sembako"));
        listKebersihan.setAll(filterKat(semua, "Kebersihan"));
        listLainnya.setAll(semua.stream()
            .filter(b -> {
                String k = b.getKategori();
                if (k == null || k.isBlank()) return true;
                for (String kp : KATEGORI_TETAP) if (kp.equalsIgnoreCase(k)) return false;
                return true;
            }).collect(Collectors.toList()));
    }

    private void cekStokRendah() {
        List<Barang> rendah = barangService.getBarangStokRendah();
        if (rendah.isEmpty()) {
            lblStokWarning.setText("");
        } else {
            String nama = rendah.stream().map(Barang::getNamaBarang)
                .limit(5).collect(Collectors.joining(", "));
            lblStokWarning.setText("Stok rendah: " + nama
                + (rendah.size() > 5 ? " dan " + (rendah.size()-5) + " lainnya." : "."));
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
