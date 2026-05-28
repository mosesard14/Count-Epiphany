package com.countepiphany.controller;

import com.countepiphany.model.DetailTransaksi;
import com.countepiphany.model.Transaksi;
import com.countepiphany.service.StrukService;
import com.countepiphany.util.AlertUtil;
import com.countepiphany.util.CurrencyUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;

/**
 * PdfPreviewController — Controller window preview struk.
 *
 * Cara pakai dari TransaksiController setelah transaksi selesai:
 *
 *   FXMLLoader loader = new FXMLLoader(
 *       MainApp.class.getResource("/com/countepiphany/fxml/pdf_preview.fxml"));
 *   Parent root = loader.load();
 *   PdfPreviewController ctrl = loader.getController();
 *
 *   Stage previewStage = new Stage();
 *   previewStage.setScene(new Scene(root));
 *   previewStage.setTitle("Struk Transaksi");
 *   previewStage.initOwner(MainApp.getPrimaryStage());
 *   previewStage.show();
 *
 *   ctrl.tampilkan(transaksi);
 */
public class PdfPreviewController {

    @FXML private ScrollPane scrollPreview;
    @FXML private VBox       vboxStruk;
    @FXML private Label      lblLoading;
    @FXML private Label      lblStatus;
    @FXML private Label      lblNamaFile;

    private final StrukService strukService = new StrukService();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Transaksi transaksi;
    private String    pdfPath;

    /**
     * Entry point. Dipanggil dari TransaksiController setelah transaksi selesai.
     */
    public void tampilkan(Transaksi transaksi) {
        this.transaksi = transaksi;
        this.pdfPath   = System.getProperty("java.io.tmpdir")
                + File.separator + "struk_" + transaksi.getIdTransaksi() + ".pdf";

        lblNamaFile.setText("struk_" + transaksi.getIdTransaksi() + ".pdf");
        lblStatus.setText("Membuat PDF...");

        // Generate PDF di background agar UI tidak freeze
        Thread t = new Thread(() -> {
            try {
                strukService.cetakStruk(transaksi, pdfPath);
                Platform.runLater(() -> {
                    renderStrukTeks();
                    lblStatus.setText("PDF siap. Klik 'Buka PDF' untuk membuka di luar app.");
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    lblLoading.setText("Gagal membuat PDF: " + e.getMessage());
                    lblStatus.setText("Error: " + e.getMessage());
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    /**
     * Render struk sebagai tampilan teks terformat di dalam ScrollPane.
     * Menggunakan font monospace agar kolom angka rata.
     */
    private void renderStrukTeks() {
        vboxStruk.getChildren().clear();

        // Kotak putih seperti kertas struk thermal
        VBox kertas = new VBox(4);
        kertas.setAlignment(Pos.TOP_CENTER);
        kertas.setStyle(
            "-fx-background-color: white;" +
            "-fx-padding: 24 20 24 20;" +
            "-fx-min-width: 300px;" +
            "-fx-max-width: 360px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 8, 0, 0, 2);"
        );

        String garis = "- - - - - - - - - - - - - - - - -";

        // Header
        kertas.getChildren().addAll(
            lbl("COUNT EPIPHANY",         "bold",   15, "#2D2D2D", true),
            lbl("Sistem Kasir Modern",    "normal", 10, "#9E9E9E", true),
            lbl(garis,                   "normal",  9, "#BDBDBD", true)
        );

        // Info transaksi
        String tgl    = transaksi.getTanggal() != null ? transaksi.getTanggal().format(FMT) : "-";
        String kasir  = transaksi.getNamaKasir() != null ? transaksi.getNamaKasir() : "Kasir";
        String metode = transaksi.getMetodeBayar() == Transaksi.MetodeBayar.cash ? "Tunai" : "Non-Tunai";

        kertas.getChildren().addAll(
            baris("No Transaksi", "#" + String.format("%05d", transaksi.getIdTransaksi())),
            baris("Tanggal",      tgl),
            baris("Kasir",        kasir),
            baris("Pembayaran",   metode),
            lbl(garis, "normal", 9, "#BDBDBD", true)
        );

        // Header kolom
        kertas.getChildren().addAll(
            lbl(String.format("%-22s %4s %10s", "Barang", "Qty", "Subtotal"), "bold", 11, "#2D2D2D", false),
            lbl("─────────────────────────────────", "normal", 9, "#BDBDBD", false)
        );

        // Item belanja
        for (DetailTransaksi item : transaksi.getDetailList()) {
            String nama = item.getNamaBarang().length() > 20
                    ? item.getNamaBarang().substring(0, 19) + "." : item.getNamaBarang();
            kertas.getChildren().add(
                lbl(String.format("%-22s %3dx %,9.0f", nama, item.getJumlah(), item.getSubtotal()),
                    "normal", 11, "#2D2D2D", false)
            );
            if (item.getJumlah() > 1) {
                kertas.getChildren().add(
                    lbl("  @Rp " + String.format("%,.0f", item.getHargaSatuan()),
                        "normal", 10, "#9E9E9E", false)
                );
            }
            if (item.getDiskonItem() > 0) {
                kertas.getChildren().add(
                    lbl("  Diskon " + (int) item.getDiskonItem() + "%",
                        "normal", 10, "#EF5350", false)
                );
            }
        }

        kertas.getChildren().add(lbl("─────────────────────────────────", "normal", 9, "#BDBDBD", false));

        // Diskon keseluruhan
        if (transaksi.getDiskon() > 0) {
            double nilaiDiskon = transaksi.getTotalHarga()
                    / (1 - transaksi.getDiskon() / 100) * (transaksi.getDiskon() / 100);
            kertas.getChildren().add(
                baris("Diskon " + (int) transaksi.getDiskon() + "%",
                      "- Rp " + String.format("%,.0f", nilaiDiskon))
            );
        }

        // Total, bayar, kembalian
        kertas.getChildren().addAll(
            barisTebal("TOTAL",   "Rp " + String.format("%,.0f", transaksi.getTotalHarga())),
            baris("Dibayar",      "Rp " + String.format("%,.0f", transaksi.getJumlahBayar())),
            barisHijau("Kembali", "Rp " + String.format("%,.0f", transaksi.getKembalian())),
            lbl(garis, "normal", 9, "#BDBDBD", true),
            lbl("Terima kasih sudah berbelanja!", "normal", 11, "#9E9E9E", true),
            lbl("Simpan struk ini sebagai bukti.",  "normal", 10, "#BDBDBD", true)
        );

        vboxStruk.getChildren().add(kertas);
    }

    // ── Tombol aksi ───────────────────────────────────────

    @FXML
    private void handleCetak() {
        if (pdfPath == null) return;
        File file = new File(pdfPath);
        if (!file.exists()) { lblStatus.setText("File PDF belum siap."); return; }
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
                lblStatus.setText("PDF dibuka di aplikasi eksternal.");
            } else {
                AlertUtil.showWarning("Tidak Didukung", "File ada di: " + pdfPath);
            }
        } catch (IOException e) {
            AlertUtil.showError("Gagal Buka", e.getMessage());
        }
    }

    @FXML
    private void handleSimpan() {
        if (pdfPath == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Simpan Struk PDF");
        chooser.setInitialFileName("struk_" + transaksi.getIdTransaksi() + ".pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        Stage stage = (Stage) scrollPreview.getScene().getWindow();
        File dest = chooser.showSaveDialog(stage);
        if (dest != null) {
            try {
                Files.copy(new File(pdfPath).toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                lblStatus.setText("Disimpan ke: " + dest.getAbsolutePath());
                AlertUtil.showInfo("Tersimpan", "PDF berhasil disimpan.");
            } catch (IOException e) {
                AlertUtil.showError("Gagal Simpan", e.getMessage());
            }
        }
    }

    @FXML
    private void handleTutup() {
        ((Stage) scrollPreview.getScene().getWindow()).close();
    }

    // ── Helper builder label ──────────────────────────────

    private Label lbl(String teks, String weight, int size, String warna, boolean center) {
        Label l = new Label(teks);
        l.setStyle(String.format(
            "-fx-font-family:'Courier New',monospace;" +
            "-fx-font-size:%dpx;-fx-font-weight:%s;-fx-text-fill:%s;",
            size, weight, warna));
        if (center) l.setAlignment(Pos.CENTER);
        l.setMaxWidth(320);
        return l;
    }

    private Label baris(String kiri, String kanan) {
        return lbl(String.format("%-18s %s", kiri, kanan), "normal", 11, "#2D2D2D", false);
    }

    private Label barisTebal(String kiri, String kanan) {
        return lbl(String.format("%-18s %s", kiri, kanan), "bold", 12, "#2D2D2D", false);
    }

    private Label barisHijau(String kiri, String kanan) {
        return lbl(String.format("%-18s %s", kiri, kanan), "bold", 12, "#43A047", false);
    }
}
