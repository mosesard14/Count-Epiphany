package com.countepiphany.util;

import com.countepiphany.dao.KasirDAO;
import com.countepiphany.model.Kasir;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;

/**
 * Utility untuk generate BCrypt hash dari password plaintext.
 * Jalankan sekali untuk hash password yang ada di database.
 */
public class PasswordHasher {
    public static void main(String[] args) {
        System.out.println("=== Password Hasher ===\n");

        // Cek user yang ada di database
        KasirDAO kasirDAO = new KasirDAO();
        List<Kasir> users = kasirDAO.findAll();

        if (users.isEmpty()) {
            System.out.println("Tidak ada user di database!");
            System.out.println("Silakan insert user manual terlebih dahulu:");
            System.out.println("INSERT INTO kasir (nama_kasir, username, password) VALUES ('Admin', 'admin', 'plaintext_password');");
            return;
        }

        System.out.println("User yang ditemukan di database:");
        for (Kasir k : users) {
            System.out.println("- ID: " + k.getIdKasir() + ", Username: " + k.getUsername() + ", Nama: " + k.getNamaKasir());
        }

        System.out.println("\n--- Generate Hash untuk Password Baru ---");
        String plainPassword = "admin123"; // Ganti dengan password Anda
        String hashed = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));

        System.out.println("Password: " + plainPassword);
        System.out.println("Hashed: " + hashed);
        System.out.println("\nUpdate database dengan query:");
        System.out.println("UPDATE kasir SET password = '" + hashed + "' WHERE username = 'admin';");
    }
}
