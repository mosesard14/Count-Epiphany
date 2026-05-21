package com.countepiphany;

import com.countepiphany.util.DatabaseConnection;
import com.countepiphany.util.DatabaseInitializer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MainApp — Titik masuk aplikasi Count Epiphany.
 * Mengelola navigasi antar scene (halaman).
 */
public class MainApp extends Application {

    private static final Logger LOGGER = Logger.getLogger(MainApp.class.getName());
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        primaryStage.setTitle("Count Epiphany — Manajemen Sistem Kasir");
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(680);

        // Uji koneksi database saat startup
        if (!DatabaseConnection.testConnection()) {
            LOGGER.warning("Koneksi database gagal — pastikan MySQL berjalan dan konfigurasi benar.");
        }

        // Auto-setup data awal (admin default) jika belum ada
        DatabaseInitializer.initialize();

        // Tampilkan halaman login
        navigateTo("login.fxml");
        primaryStage.show();
    }

    /**
     * Menavigasi ke halaman berdasarkan nama file FXML.
     *
     * @param fxmlFile nama file FXML di direktori resources/fxml/
     */
    public static void navigateTo(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    MainApp.class.getResource("/com/countepiphany/fxml/" + fxmlFile));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    MainApp.class.getResource("/com/countepiphany/css/style.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Gagal memuat halaman: " + fxmlFile, e);
        }
    }

    /**
     * Menavigasi ke halaman dan mengembalikan controller-nya.
     */
    public static <T> T navigateToWithController(String fxmlFile) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                MainApp.class.getResource("/com/countepiphany/fxml/" + fxmlFile));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(
                MainApp.class.getResource("/com/countepiphany/css/style.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
        return loader.getController();
    }

    public static Stage getPrimaryStage() { return primaryStage; }

    public static void main(String[] args) {
        launch(args);
    }
}
