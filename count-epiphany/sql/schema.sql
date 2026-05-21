-- ============================================================
-- Count Epiphany - Database Schema
-- Sistem Manajemen Kasir
-- ============================================================

CREATE DATABASE IF NOT EXISTS count_epiphany
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE count_epiphany;

-- ============================================================
-- 1. Tabel KASIR
-- Menyimpan data pengguna/petugas kasir
-- ============================================================
CREATE TABLE IF NOT EXISTS kasir (
    id_kasir   INT          NOT NULL AUTO_INCREMENT,
    nama_kasir VARCHAR(100) NOT NULL,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,          -- BCrypt hash
    CONSTRAINT pk_kasir PRIMARY KEY (id_kasir)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 2. Tabel SUPPLIER
-- Menyimpan data pemasok/vendor barang
-- ============================================================
CREATE TABLE IF NOT EXISTS supplier (
    id_supplier   VARCHAR(20)  NOT NULL,
    nama_supplier VARCHAR(100) NOT NULL,
    alamat        TEXT,
    telepon       VARCHAR(20),
    email         VARCHAR(100),
    CONSTRAINT pk_supplier PRIMARY KEY (id_supplier)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 3. Tabel BARANG
-- Inti sistem inventori — stok hanya berubah via pembelian/transaksi
-- ============================================================
CREATE TABLE IF NOT EXISTS barang (
    id_barang   VARCHAR(20)    NOT NULL,
    nama_barang VARCHAR(150)   NOT NULL,
    harga_beli  DECIMAL(15, 2) NOT NULL DEFAULT 0,
    harga_jual  DECIMAL(15, 2) NOT NULL DEFAULT 0,
    stok        INT            NOT NULL DEFAULT 0,
    kategori    VARCHAR(50),
    id_supplier VARCHAR(20),
    stok_minimum INT           NOT NULL DEFAULT 5,   -- ambang batas peringatan stok
    CONSTRAINT pk_barang      PRIMARY KEY (id_barang),
    CONSTRAINT fk_barang_sup  FOREIGN KEY (id_supplier)
        REFERENCES supplier(id_supplier)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 4. Tabel PEMBELIAN_BARANG
-- Satu-satunya jalur penambahan stok (restock dari supplier)
-- ============================================================
CREATE TABLE IF NOT EXISTS pembelian_barang (
    id_pembelian      INT            NOT NULL AUTO_INCREMENT,
    tanggal           DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_supplier       VARCHAR(20)    NOT NULL,
    id_barang         VARCHAR(20)    NOT NULL,
    jumlah_beli       INT            NOT NULL,
    harga_beli_satuan DECIMAL(15, 2) NOT NULL,
    total_modal       DECIMAL(15, 2) GENERATED ALWAYS AS (jumlah_beli * harga_beli_satuan) STORED,
    id_kasir          INT            NOT NULL,
    CONSTRAINT pk_pembelian     PRIMARY KEY (id_pembelian),
    CONSTRAINT fk_pemb_supplier FOREIGN KEY (id_supplier)
        REFERENCES supplier(id_supplier) ON UPDATE CASCADE,
    CONSTRAINT fk_pemb_barang   FOREIGN KEY (id_barang)
        REFERENCES barang(id_barang)     ON UPDATE CASCADE,
    CONSTRAINT fk_pemb_kasir    FOREIGN KEY (id_kasir)
        REFERENCES kasir(id_kasir)       ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 5. Tabel TRANSAKSI
-- Header setiap transaksi penjualan
-- ============================================================
CREATE TABLE IF NOT EXISTS transaksi (
    id_transaksi INT            NOT NULL AUTO_INCREMENT,
    tanggal      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_harga  DECIMAL(15, 2) NOT NULL DEFAULT 0,
    diskon       DECIMAL(5, 2)  NOT NULL DEFAULT 0,   -- persen diskon (0-100)
    jumlah_bayar DECIMAL(15, 2) NOT NULL DEFAULT 0,
    kembalian    DECIMAL(15, 2) NOT NULL DEFAULT 0,
    metode_bayar ENUM('cash','cashless') NOT NULL DEFAULT 'cash',
    id_kasir     INT            NOT NULL,
    CONSTRAINT pk_transaksi    PRIMARY KEY (id_transaksi),
    CONSTRAINT fk_trx_kasir   FOREIGN KEY (id_kasir)
        REFERENCES kasir(id_kasir) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 6. Tabel DETAIL_TRANSAKSI
-- Rincian item barang dalam setiap transaksi
-- ============================================================
CREATE TABLE IF NOT EXISTS detail_transaksi (
    id_detail    INT            NOT NULL AUTO_INCREMENT,
    id_transaksi INT            NOT NULL,
    id_barang    VARCHAR(20)    NOT NULL,
    jumlah       INT            NOT NULL,
    harga_satuan DECIMAL(15, 2) NOT NULL,   -- snapshot harga saat transaksi
    diskon_item  DECIMAL(5, 2)  NOT NULL DEFAULT 0,
    subtotal     DECIMAL(15, 2) NOT NULL,
    CONSTRAINT pk_detail      PRIMARY KEY (id_detail),
    CONSTRAINT fk_det_trx    FOREIGN KEY (id_transaksi)
        REFERENCES transaksi(id_transaksi) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_det_barang FOREIGN KEY (id_barang)
        REFERENCES barang(id_barang)       ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 7. Tabel LAPORAN_KEUANGAN
-- Rangkuman pendapatan per periode
-- ============================================================
CREATE TABLE IF NOT EXISTS laporan_keuangan (
    id_laporan       INT            NOT NULL AUTO_INCREMENT,
    tanggal_mulai    DATE           NOT NULL,
    tanggal_selesai  DATE           NOT NULL,
    total_pendapatan DECIMAL(15, 2) NOT NULL DEFAULT 0,
    dibuat_pada      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_kasir         INT,
    CONSTRAINT pk_laporan    PRIMARY KEY (id_laporan),
    CONSTRAINT fk_lap_kasir FOREIGN KEY (id_kasir)
        REFERENCES kasir(id_kasir) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- TRIGGERS — menjaga integritas stok otomatis
-- ============================================================

-- Trigger: Tambah stok setelah pembelian barang disimpan
DELIMITER $$
CREATE TRIGGER trg_after_insert_pembelian
AFTER INSERT ON pembelian_barang
FOR EACH ROW
BEGIN
    UPDATE barang
    SET stok = stok + NEW.jumlah_beli
    WHERE id_barang = NEW.id_barang;
END$$
DELIMITER ;

-- Trigger: Kurangi stok setelah detail transaksi penjualan disimpan
DELIMITER $$
CREATE TRIGGER trg_after_insert_detail_transaksi
AFTER INSERT ON detail_transaksi
FOR EACH ROW
BEGIN
    UPDATE barang
    SET stok = stok - NEW.jumlah
    WHERE id_barang = NEW.id_barang;
END$$
DELIMITER ;

-- Trigger: Kembalikan stok jika detail transaksi dihapus (pembatalan)
DELIMITER $$
CREATE TRIGGER trg_after_delete_detail_transaksi
AFTER DELETE ON detail_transaksi
FOR EACH ROW
BEGIN
    UPDATE barang
    SET stok = stok + OLD.jumlah
    WHERE id_barang = OLD.id_barang;
END$$
DELIMITER ;

-- ============================================================
-- DATA AWAL
-- TIDAK perlu INSERT manual di sini.
-- DatabaseInitializer.java otomatis membuat user admin='admin123'
-- saat aplikasi pertama kali dijalankan (jika belum ada).
--
-- Jika ingin insert manual (misal via MySQL CLI), jalankan:
-- UPDATE kasir SET password = '<hash_baru>' WHERE username = 'admin';
-- Hash baru bisa di-generate via PasswordHasher.java
-- ============================================================

-- ============================================================
-- INDEXES untuk performa query
-- ============================================================
CREATE INDEX idx_barang_kategori      ON barang(kategori);
CREATE INDEX idx_barang_stok          ON barang(stok);
CREATE INDEX idx_transaksi_tanggal    ON transaksi(tanggal);
CREATE INDEX idx_pembelian_tanggal    ON pembelian_barang(tanggal);
CREATE INDEX idx_detail_transaksi_id  ON detail_transaksi(id_transaksi);
CREATE INDEX idx_laporan_periode      ON laporan_keuangan(tanggal_mulai, tanggal_selesai);
