package com.countepiphany.controller;

import com.countepiphany.MainApp;
import com.countepiphany.service.AuthService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

/**
 * LoginController — Controller untuk halaman login.
 */
public class LoginController {

    @FXML private TextField     txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label         lblError;

    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        lblError.setVisible(false);

        // Enter di field username → pindah ke password
        txtUsername.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) txtPassword.requestFocus();
        });

        // Enter di field password → langsung login
        txtPassword.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) handleLogin();
        });

        // Auto focus ke username
        Platform.runLater(() -> txtUsername.requestFocus());
    }

    @FXML
    private void handleLogin() {
        System.out.println("=== handleLogin dipanggil ===");
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();
        System.out.println("Username: " + username);
        System.out.println("Password length: " + password.length());

        if (username.isEmpty() || password.isEmpty()) {
            System.out.println("Username atau password kosong");
            showError("Username dan password tidak boleh kosong.");
            return;
        }

        System.out.println("Mencoba login...");
        boolean berhasil = authService.login(username, password);
        System.out.println("Hasil login: " + berhasil);

        if (berhasil) {
            System.out.println("Login berhasil, navigasi ke transaksi");
            lblError.setVisible(false);
            MainApp.navigateTo("transaksi.fxml");
        } else {
            System.out.println("Login gagal");
            showError("Username atau password salah.");
            txtPassword.clear();
            txtPassword.requestFocus();
        }
    }

    private void showError(String pesan) {
        lblError.setText(pesan);
        lblError.setVisible(true);
    }
}
