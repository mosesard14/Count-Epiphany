package com.countepiphany;

import com.countepiphany.util.DatabaseConnection;
import com.countepiphany.util.DatabaseInitializer;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
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
    private static double lastWidth = 1100;
    private static double lastHeight = 720;
    private static boolean wasMaximized = false;
    private static boolean wasFullScreen = false;

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

        // Track window state changes
        primaryStage.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (!primaryStage.isMaximized() && !primaryStage.isFullScreen()) {
                lastWidth = newVal.doubleValue();
            }
        });

        primaryStage.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (!primaryStage.isMaximized() && !primaryStage.isFullScreen()) {
                lastHeight = newVal.doubleValue();
            }
        });

        primaryStage.maximizedProperty().addListener((obs, oldVal, newVal) -> {
            wasMaximized = newVal;
        });

        primaryStage.fullScreenProperty().addListener((obs, oldVal, newVal) -> {
            wasFullScreen = newVal;
        });
    }

    /**
     * Menavigasi ke halaman berdasarkan nama file FXML.
     *
     * @param fxmlFile nama file FXML di direktori resources/fxml/
     */
    public static void navigateTo(String fxmlFile) {
        try {
            // Simpan state window saat ini SEBELUM navigasi
            boolean isMaximized = primaryStage.isMaximized();
            boolean isFullScreen = primaryStage.isFullScreen();

            // Update ukuran terakhir dari window saat ini jika tidak maximized/fullscreen
            if (!isMaximized && !isFullScreen && primaryStage.getWidth() > 0 && primaryStage.getHeight() > 0) {
                lastWidth = primaryStage.getWidth();
                lastHeight = primaryStage.getHeight();
            }

            // Gunakan ukuran yang konsisten
            double width = lastWidth > 0 ? lastWidth : 1100;
            double height = lastHeight > 0 ? lastHeight : 720;

            FXMLLoader loader = new FXMLLoader(
                    MainApp.class.getResource("/com/countepiphany/fxml/" + fxmlFile));
            Parent root = loader.load();
            Scene scene = new Scene(root, width, height);
            scene.getStylesheets().add(
                    MainApp.class.getResource("/com/countepiphany/css/style.css").toExternalForm());

            primaryStage.setScene(scene);

            // Restore state window SETELAH set scene
            if (isFullScreen) {
                primaryStage.setFullScreen(true);
            } else if (isMaximized) {
                primaryStage.setMaximized(true);
            } else {
                primaryStage.setWidth(width);
                primaryStage.setHeight(height);
                primaryStage.centerOnScreen();
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Gagal memuat halaman: " + fxmlFile, e);
        }
    }

    /**
     * Menavigasi ke halaman dan mengembalikan controller-nya.
     */
    public static <T> T navigateToWithController(String fxmlFile) throws IOException {
        // Simpan state window saat ini SEBELUM navigasi
        boolean isMaximized = primaryStage.isMaximized();
        boolean isFullScreen = primaryStage.isFullScreen();

        // Update ukuran terakhir dari window saat ini jika tidak maximized/fullscreen
        if (!isMaximized && !isFullScreen && primaryStage.getWidth() > 0 && primaryStage.getHeight() > 0) {
            lastWidth = primaryStage.getWidth();
            lastHeight = primaryStage.getHeight();
        }

        // Gunakan ukuran yang konsisten
        double width = lastWidth > 0 ? lastWidth : 1100;
        double height = lastHeight > 0 ? lastHeight : 720;

        FXMLLoader loader = new FXMLLoader(
                MainApp.class.getResource("/com/countepiphany/fxml/" + fxmlFile));
        Parent root = loader.load();
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(
                MainApp.class.getResource("/com/countepiphany/css/style.css").toExternalForm());

        primaryStage.setScene(scene);

        // Restore state window SETELAH set scene
        if (isFullScreen) {
            primaryStage.setFullScreen(true);
        } else if (isMaximized) {
            primaryStage.setMaximized(true);
        } else {
            primaryStage.setWidth(width);
            primaryStage.setHeight(height);
            primaryStage.centerOnScreen();
        }
        return loader.getController();
    }

    public static Stage getPrimaryStage() { return primaryStage; }

    public static void main(String[] args) {
        launch(args);
    }
}
