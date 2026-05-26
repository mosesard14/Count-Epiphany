package com.countepiphany.service;

import com.countepiphany.model.DetailTransaksi;
import com.countepiphany.model.Transaksi;
import com.countepiphany.util.CurrencyUtil;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.DashedBorder;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * StrukService — Struk PDF terformat rapi dengan tabel kolom dan tipografi berjenjang.
 * Info toko dibaca dari toko.properties, tidak perlu ubah kode ini.
 */
public class StrukService {

    private static final Logger            LOGGER  = Logger.getLogger(StrukService.class.getName());
    private static final DateTimeFormatter FMT_TGL = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_WKT = DateTimeFormatter.ofPattern("HH:mm");

    // Warna
    private static final DeviceRgb WARNA_GELAP  = new DeviceRgb(30, 30, 30);
    private static final DeviceRgb WARNA_MUTED  = new DeviceRgb(100, 100, 100);
    private static final DeviceRgb WARNA_GARIS  = new DeviceRgb(180, 180, 180);

    // Info toko dari toko.properties
    private final String namaToko;
    private final String tagline;
    private final String alamat;
    private final String telepon;
    private final String footer;

    public StrukService() {
        Properties props = new Properties();
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("toko.properties")) {
            if (in != null) props.load(in);
            else LOGGER.warning("toko.properties tidak ditemukan.");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Gagal baca toko.properties", e);
        }
        this.namaToko = props.getProperty("toko.nama",    "Nama Toko");
        this.tagline  = props.getProperty("toko.tagline", "");
        this.alamat   = props.getProperty("toko.alamat",  "");
        this.telepon  = props.getProperty("toko.telepon", "");
        this.footer   = props.getProperty("toko.footer",  "Terima kasih!");
    }

    public void cetakStruk(Transaksi transaksi, String outputPath) throws IOException {
        // Lebar struk thermal 80mm = 226pt
        PageSize ukuran = new PageSize(226, 800);

        try (PdfWriter   writer = new PdfWriter(outputPath);
             PdfDocument pdf    = new PdfDocument(writer);
             Document    doc    = new Document(pdf, ukuran)) {

            doc.setMargins(14, 12, 14, 12);

            // ── HEADER ─────────────────────────────────────────
            doc.add(new Paragraph(namaToko.toUpperCase())
                    .setFontSize(13).setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(WARNA_GELAP)
                    .setMarginBottom(2));

            if (!tagline.isBlank())
                doc.add(teksKecil(tagline, TextAlignment.CENTER));
            if (!alamat.isBlank())
                doc.add(teksKecil(alamat, TextAlignment.CENTER));
            if (!telepon.isBlank())
                doc.add(teksKecil("Telp: " + telepon, TextAlignment.CENTER));

            doc.add(garisDash());

            // ── INFO TRANSAKSI ──────────────────────────────────
            // Baris 1: No TRX kiri, tanggal kanan
            Table tInfo1 = tabelDua();
            tInfo1.addCell(selKiri("No  : TRX-" + String.format("%05d", transaksi.getIdTransaksi()), 8));
            tInfo1.addCell(selKanan(
                    transaksi.getTanggal() != null ? transaksi.getTanggal().format(FMT_TGL) : "-", 8));
            doc.add(tInfo1);

            // Baris 2: kasir kiri, jam kanan
            Table tInfo2 = tabelDua();
            tInfo2.addCell(selKiri("Kasir: " + nvl(transaksi.getNamaKasir()), 8));
            tInfo2.addCell(selKanan(
                    transaksi.getTanggal() != null ? transaksi.getTanggal().format(FMT_WKT) : "-", 8));
            doc.add(tInfo2);

            // Baris 3: metode
            doc.add(teksKecil("Metode: " + (transaksi.getMetodeBayar() == Transaksi.MetodeBayar.cash
                    ? "Tunai (Cash)" : "Non-Tunai (Cashless)"), TextAlignment.LEFT));

            doc.add(garisDash());

            // ── HEADER KOLOM TABEL ITEM ─────────────────────────
            Table hdrKolom = tabelItem();
            hdrKolom.addCell(hdrSel("NAMA BARANG", TextAlignment.LEFT));
            hdrKolom.addCell(hdrSel("QTY", TextAlignment.CENTER));
            hdrKolom.addCell(hdrSel("HARGA", TextAlignment.RIGHT));
            hdrKolom.addCell(hdrSel("SUBTOTAL", TextAlignment.RIGHT));
            doc.add(hdrKolom);

            doc.add(garisSolid());

            // ── DAFTAR ITEM ─────────────────────────────────────
            for (DetailTransaksi item : transaksi.getDetailList()) {
                Table baris = tabelItem();
                baris.addCell(selItem(item.getNamaBarang(), TextAlignment.LEFT));
                baris.addCell(selItem(String.valueOf(item.getJumlah()), TextAlignment.CENTER));
                baris.addCell(selItem(formatAngka(item.getHargaSatuan()), TextAlignment.RIGHT));
                baris.addCell(selItem(formatAngka(item.getSubtotal()), TextAlignment.RIGHT));
                doc.add(baris);

                if (item.getDiskonItem() > 0) {
                    doc.add(new Paragraph(String.format("  disc %.0f%%", item.getDiskonItem()))
                            .setFontSize(7).setFontColor(WARNA_MUTED)
                            .setMarginTop(0).setMarginBottom(2));
                }
            }

            doc.add(garisDash());

            // ── RINGKASAN ───────────────────────────────────────
            if (transaksi.getDiskon() > 0) {
                double subtotalKotor = transaksi.getDetailList().stream()
                        .mapToDouble(DetailTransaksi::getSubtotal).sum();
                doc.add(barisRingkasan(
                        "Diskon (" + (int) transaksi.getDiskon() + "%)",
                        "- " + formatAngka(subtotalKotor - transaksi.getTotalHarga()),
                        8, false));
            }

            // Subtotal
            double subtotal = transaksi.getDetailList().stream()
                    .mapToDouble(DetailTransaksi::getSubtotal).sum();
            doc.add(barisRingkasan("Subtotal", formatAngka(subtotal), 8, false));

            doc.add(garisSolid());

            // TOTAL — besar dan tebal
            Table tTotal = tabelDua();
            tTotal.addCell(new Cell().add(new Paragraph("TOTAL")
                            .setFontSize(11).setBold().setFontColor(WARNA_GELAP))
                    .setBorder(Border.NO_BORDER).setPaddingTop(4).setPaddingBottom(4));
            tTotal.addCell(new Cell().add(new Paragraph("Rp " + formatAngka(transaksi.getTotalHarga()))
                            .setFontSize(11).setBold().setFontColor(WARNA_GELAP)
                            .setTextAlignment(TextAlignment.RIGHT))
                    .setBorder(Border.NO_BORDER).setPaddingTop(4).setPaddingBottom(4));
            doc.add(tTotal);

            doc.add(garisSolid());

            doc.add(barisRingkasan("Tunai", formatAngka(transaksi.getJumlahBayar()), 8, false));
            doc.add(barisRingkasan("Kembali", "Rp " + formatAngka(transaksi.getKembalian()), 9, true));

            doc.add(garisDash());

            // ── FOOTER ──────────────────────────────────────────
            doc.add(teksKecil(footer, TextAlignment.CENTER));
            doc.add(teksKecil("Barang yang sudah dibeli", TextAlignment.CENTER));
            doc.add(teksKecil("tidak dapat dikembalikan", TextAlignment.CENTER));
            doc.add(new Paragraph("* * * * *")
                    .setFontSize(8).setFontColor(WARNA_MUTED)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(6));
        }
    }

    // ── Helper: tabel 2 kolom (50/50) ───────────────────────────
    private Table tabelDua() {
        Table t = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .setWidth(UnitValue.createPercentValue(100));
        t.setMarginBottom(2);
        return t;
    }

    // ── Helper: tabel item 4 kolom ───────────────────────────────
    private Table tabelItem() {
        Table t = new Table(UnitValue.createPercentArray(new float[]{44, 10, 22, 24}))
                .setWidth(UnitValue.createPercentValue(100));
        t.setMarginBottom(1);
        return t;
    }

    private Cell selKiri(String teks, float size) {
        return new Cell().add(new Paragraph(teks).setFontSize(size).setFontColor(WARNA_GELAP))
                .setBorder(Border.NO_BORDER).setPaddingBottom(1);
    }

    private Cell selKanan(String teks, float size) {
        return new Cell().add(new Paragraph(teks).setFontSize(size).setFontColor(WARNA_GELAP)
                        .setTextAlignment(TextAlignment.RIGHT))
                .setBorder(Border.NO_BORDER).setPaddingBottom(1);
    }

    private Cell hdrSel(String teks, TextAlignment align) {
        return new Cell().add(new Paragraph(teks).setFontSize(7).setBold()
                        .setFontColor(WARNA_MUTED).setTextAlignment(align))
                .setBorder(Border.NO_BORDER).setPaddingBottom(3);
    }

    private Cell selItem(String teks, TextAlignment align) {
        return new Cell().add(new Paragraph(teks).setFontSize(8)
                        .setFontColor(WARNA_GELAP).setTextAlignment(align))
                .setBorder(Border.NO_BORDER).setPaddingBottom(4);
    }

    private Table barisRingkasan(String label, String nilai, float size, boolean bold) {
        Table t = tabelDua();
        Cell cLabel = new Cell().add(new Paragraph(label).setFontSize(size)
                .setFontColor(WARNA_GELAP));
        Cell cNilai = new Cell().add(new Paragraph(nilai).setFontSize(size)
                .setFontColor(WARNA_GELAP).setTextAlignment(TextAlignment.RIGHT));
        if (bold) { cLabel.setBold(); cNilai.setBold(); }
        t.addCell(cLabel.setBorder(Border.NO_BORDER).setPaddingBottom(2));
        t.addCell(cNilai.setBorder(Border.NO_BORDER).setPaddingBottom(2));
        return t;
    }

    private Paragraph teksKecil(String teks, TextAlignment align) {
        return new Paragraph(teks).setFontSize(8).setFontColor(WARNA_MUTED)
                .setTextAlignment(align).setMarginBottom(1).setMarginTop(0);
    }

    private Paragraph garisDash() {
        return new Paragraph("- ".repeat(25).trim())
                .setFontSize(7).setFontColor(WARNA_GARIS)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(5).setMarginBottom(5);
    }

    private Paragraph garisSolid() {
        return new Paragraph("─".repeat(34))
                .setFontSize(7).setFontColor(WARNA_GARIS)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(3).setMarginBottom(3);
    }

    private String formatAngka(double angka) {
        return CurrencyUtil.formatPlain(angka);
    }

    private String nvl(String s) {
        return s != null ? s : "-";
    }
}
