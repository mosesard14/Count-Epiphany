package com.countepiphany.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * CurrencyUtil — Pembantu format mata uang Rupiah.
 */
public final class CurrencyUtil {

    private static final DecimalFormatSymbols SYMBOLS;
    private static final DecimalFormat FORMATTER;

    static {
        SYMBOLS = new DecimalFormatSymbols(new Locale("id", "ID"));
        SYMBOLS.setGroupingSeparator('.');
        SYMBOLS.setDecimalSeparator(',');
        FORMATTER = new DecimalFormat("#,##0.00", SYMBOLS);
    }

    private CurrencyUtil() {}

    /**
     * Memformat angka menjadi string Rupiah, contoh: "Rp 58.900,00"
     */
    public static String format(double amount) {
        return "Rp " + FORMATTER.format(amount);
    }

    /**
     * Memformat angka tanpa prefiks "Rp", contoh: "58.900,00"
     */
    public static String formatPlain(double amount) {
        return FORMATTER.format(amount);
    }

    /**
     * Mem-parsing string Rupiah kembali ke double.
     * Menerima format "Rp 58.900,00" atau "58.900,00".
     */
    public static double parse(String text) {
        if (text == null || text.isBlank()) return 0.0;
        String cleaned = text.replace("Rp", "").replace(" ", "")
                             .replace(".", "").replace(",", ".");
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
