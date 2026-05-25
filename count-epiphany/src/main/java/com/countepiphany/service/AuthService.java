package com.countepiphany.service;

import com.countepiphany.dao.KasirDAO;
import com.countepiphany.model.Kasir;
import com.countepiphany.util.SessionManager;
import org.mindrot.jbcrypt.BCrypt;


import java.util.Optional;

/**
 * AuthService — Logika bisnis autentikasi (login/logout).
 * Menggunakan BCrypt untuk verifikasi password.
 */
public class AuthService {

    private final KasirDAO kasirDAO = new KasirDAO();

    /**
     * Memvalidasi kredensial dan membuat sesi jika berhasil.
     *
     * @param username username yang diinput
     * @param password password plaintext yang diinput
     * @return true jika login berhasil
     */
    public boolean login(String username, String password) {
        if (username == null || username.isBlank()
                || password == null || password.isBlank()) {
            return false;
        }

        Optional<Kasir> opt = kasirDAO.findByUsername(username.trim());
        if (opt.isEmpty()) return false;

        Kasir kasir = opt.get();
        if (!BCrypt.checkpw(password, kasir.getPassword())) return false;

        SessionManager.login(kasir);
        return true;
    }

    /**
     * Mengakhiri sesi pengguna yang sedang login.
     */
    public void logout() {
        SessionManager.logout();
    }

}
