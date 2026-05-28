package com.countepiphany.util;

import java.sql.Connection;

public class TestConnection {
    public static void main(String[] args) {
        System.out.println("=== Test Koneksi Database ===");

        if (DatabaseConnection.testConnection()) {
            System.out.println("✓ Koneksi BERHASIL!");

            try (Connection conn = DatabaseConnection.getConnection()) {
                System.out.println("✓ Database: " + conn.getCatalog());
                System.out.println("✓ URL: " + conn.getMetaData().getURL());
            } catch (Exception e) {
                System.out.println("✗ Error: " + e.getMessage());
            }
        } else {
            System.out.println("✗ Koneksi GAGAL!");
            System.out.println("\nPastikan:");
            System.out.println("1. MySQL sudah running");
            System.out.println("2. Database sudah dibuat");
            System.out.println("3. File database.properties sudah benar");
        }
    }
}
