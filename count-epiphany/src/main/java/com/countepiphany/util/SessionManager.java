package com.countepiphany.util;

import com.countepiphany.model.Kasir;

/**
 * SessionManager — Menyimpan state sesi pengguna yang sedang login.
 * Singleton sederhana berbasis static field.
 */
public final class SessionManager {

    private static Kasir currentKasir = null;

    private SessionManager() {}

    /** Menyimpan data kasir setelah login berhasil. */
    public static void login(Kasir kasir) {
        currentKasir = kasir;
    }

    /** Menghapus sesi (logout). */
    public static void logout() {
        currentKasir = null;
    }

    /** Mendapatkan kasir yang sedang login. */
    public static Kasir getCurrentKasir() {
        return currentKasir;
    }

    /** Mengecek apakah ada pengguna yang sedang login. */
    public static boolean isLoggedIn() {
        return currentKasir != null;
    }

    /** Mendapatkan ID kasir yang sedang login (shortcut). */
    public static int getCurrentKasirId() {
        if (currentKasir == null) throw new IllegalStateException("Tidak ada sesi aktif.");
        return currentKasir.getIdKasir();
    }
}
