package com.countepiphany.service;

import com.countepiphany.dao.LaporanKeuanganDAO;
import com.countepiphany.dao.TransaksiDAO;
import com.countepiphany.model.LaporanKeuangan;
import com.countepiphany.util.SessionManager;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * LaporanService — Logika bisnis pembuatan laporan keuangan dan ekspor data.
 */
public class LaporanService {

    private static final Logger      LOGGER          = Logger.getLogger(LaporanService.class.getName());
    private static final DateTimeFormatter DATE_FMT  = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String            NAMA_TOKO = "Count Epiphany";

    private final TransaksiDAO       transaksiDAO   = new TransaksiDAO();
    private final LaporanKeuanganDAO laporanDAO     = new LaporanKeuanganDAO();

    /**
     * Menghitung total pendapatan dalam rentang tanggal.
     */
    public double hitungTotalPendapatan(LocalDate dari, LocalDate sampai) {
        return transaksiDAO.sumPendapatanByPeriode(dari, sampai);
    }

    /**
     * Mengambil data laporan per barang untuk ditampilkan di tabel.
     */
    public List<Object[]> getLaporanBarang(LocalDate dari, LocalDate sampai) {
        return transaksiDAO.getLaporanBarang(dari, sampai);
    }

    /**
     * Menyimpan ringkasan laporan ke tabel laporan_keuangan.
     */
    public LaporanKeuangan simpanLaporan(LocalDate dari, LocalDate sampai) {
        double total = hitungTotalPendapatan(dari, sampai);
        LaporanKeuangan laporan = new LaporanKeuangan(
                dari, sampai, total, SessionManager.getCurrentKasirId());
        laporanDAO.save(laporan);
        return laporan;
    }

    // ── Export PDF ───────────────────────────────────────

    /**
     * Mengekspor laporan penjualan ke file PDF.
     *
     * @param filePath  path file output
     * @param dari      tanggal mulai
     * @param sampai    tanggal selesai
     * @param dataRows  baris data dari getLaporanBarang()
     * @param totalPendapatan total pendapatan periode
     */
    public void exportToPdf(String filePath, LocalDate dari, LocalDate sampai,
                             List<Object[]> dataRows, double totalPendapatan) throws IOException {
        try (PdfWriter writer = new PdfWriter(filePath);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf)) {

            PdfFont bold = PdfFontFactory.createFont("Helvetica-Bold");
            PdfFont regular = PdfFontFactory.createFont("Helvetica");

            // Header
            doc.add(new Paragraph(NAMA_TOKO)
                    .setFont(bold).setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("Laporan Penjualan")
                    .setFont(regular).setFontSize(14)
                    .setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("Periode: " + dari.format(DATE_FMT)
                    + " s/d " + sampai.format(DATE_FMT))
                    .setFont(regular).setFontSize(11)
                    .setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("\n"));

            // Tabel
            float[] widths = {1.5f, 2f, 3f, 1.5f, 1.5f, 2f, 2.5f};
            Table table = new Table(UnitValue.createPercentArray(widths))
                    .useAllAvailableWidth();

            String[] headers = {"Kode", "Kategori", "Nama Barang",
                                 "Stok", "Terjual", "Harga Jual", "Nominal Penjualan"};
            for (String h : headers) {
                table.addHeaderCell(new Cell()
                        .add(new Paragraph(h).setFont(bold).setFontSize(9))
                        .setBackgroundColor(new com.itextpdf.kernel.colors.DeviceRgb(100, 96, 197))
                        .setFontColor(ColorConstants.WHITE)
                        .setTextAlignment(TextAlignment.CENTER));
            }

            for (Object[] row : dataRows) {
                table.addCell(cell(String.valueOf(row[0]), regular));
                table.addCell(cell(String.valueOf(row[1]), regular));
                table.addCell(cell(String.valueOf(row[2]), regular));
                table.addCell(cellRight(String.valueOf(row[3]), regular));
                table.addCell(cellRight(String.valueOf(row[4]), regular));
                table.addCell(cellRight(formatRupiah((double) row[5]), regular));
                table.addCell(cellRight(formatRupiah((double) row[6]), regular));
            }

            doc.add(table);
            doc.add(new Paragraph("\n"));

            // Total
            doc.add(new Paragraph("Total Pendapatan: " + formatRupiah(totalPendapatan))
                    .setFont(bold).setFontSize(12)
                    .setTextAlignment(TextAlignment.RIGHT));
        }
    }

    // ── Export Excel ─────────────────────────────────────

    /**
     * Mengekspor laporan penjualan ke file Excel (.xlsx).
     */
    public void exportToExcel(String filePath, LocalDate dari, LocalDate sampai,
                               List<Object[]> dataRows, double totalPendapatan) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Laporan Penjualan");

            // Style
            CellStyle headerStyle = wb.createCellStyle();
            org.apache.poi.ss.usermodel.Font boldFont = wb.createFont();
            boldFont.setBold(true);
            headerStyle.setFont(boldFont);
            headerStyle.setFillForegroundColor(IndexedColors.LAVENDER.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            CellStyle currencyStyle = wb.createCellStyle();
            DataFormat df = wb.createDataFormat();
            currencyStyle.setDataFormat(df.getFormat("#,##0.00"));

            // Judul
            Row titleRow = sheet.createRow(0);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(NAMA_TOKO + " — Laporan Penjualan");
            titleCell.setCellStyle(headerStyle);

            Row periodeRow = sheet.createRow(1);
            periodeRow.createCell(0).setCellValue(
                    "Periode: " + dari.format(DATE_FMT) + " s/d " + sampai.format(DATE_FMT));

            // Header kolom
            Row headerRow = sheet.createRow(3);
            String[] cols = {"Kode Barang", "Kategori", "Nama Barang",
                              "Stok", "Terjual", "Harga Jual", "Nominal Penjualan"};
            for (int i = 0; i < cols.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(cols[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data
            int rowNum = 4;
            for (Object[] data : dataRows) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(String.valueOf(data[0]));
                row.createCell(1).setCellValue(String.valueOf(data[1]));
                row.createCell(2).setCellValue(String.valueOf(data[2]));
                row.createCell(3).setCellValue((Integer) data[3]);
                row.createCell(4).setCellValue((Integer) data[4]);
                org.apache.poi.ss.usermodel.Cell hj = row.createCell(5);
                hj.setCellValue((Double) data[5]);
                hj.setCellStyle(currencyStyle);
                org.apache.poi.ss.usermodel.Cell np = row.createCell(6);
                np.setCellValue((Double) data[6]);
                np.setCellStyle(currencyStyle);
            }

            // Total
            Row totalRow = sheet.createRow(rowNum + 1);
            org.apache.poi.ss.usermodel.Cell totalLabel = totalRow.createCell(5);
            totalLabel.setCellValue("Total Pendapatan:");
            totalLabel.setCellStyle(headerStyle);
            org.apache.poi.ss.usermodel.Cell totalVal = totalRow.createCell(6);
            totalVal.setCellValue(totalPendapatan);
            totalVal.setCellStyle(currencyStyle);

            // Auto-size kolom
            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);

            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                wb.write(fos);
            }
        }
    }

    // ── Helper ───────────────────────────────────────────

    private Cell cell(String text, PdfFont font) {
        return new Cell().add(new Paragraph(text).setFont(font).setFontSize(8));
    }

    private Cell cellRight(String text, PdfFont font) {
        return new Cell().add(new Paragraph(text).setFont(font).setFontSize(8))
                .setTextAlignment(TextAlignment.RIGHT);
    }

    private String formatRupiah(double amount) {
        return String.format("Rp %,.0f", amount);
    }
}
