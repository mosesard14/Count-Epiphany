# Count Epiphany — Aplikasi Kasir Desktop
Sistem Manajemen Kasir Native Desktop menggunakan Java + JavaFX + MySQL

---

## Prasyarat
| Tool | Versi Minimum |
|------|---------------|
| Java JDK | 17 |
| Maven | 3.8+ |
| MySQL | 8.0+ |
| IntelliJ IDEA | 2023+ (disarankan) |

---

## Langkah Setup

### 1. Import Database
Buka phpMyAdmin atau MySQL CLI, lalu jalankan:
```sql
source /path/to/count-epiphany/sql/schema.sql
```
Atau lewat MySQL CLI:
```bash
mysql -u root -p < sql/schema.sql
```

### 2. Konfigurasi Database
Edit file `src/main/resources/database.properties`:
```properties
db.url=jdbc:mysql://localhost:3306/count_epiphany?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Jakarta
db.username=root
db.password=PASSWORD_ANDA
```

### 3. Build & Run
```bash
mvn clean javafx:run
```
Atau di IntelliJ: klik kanan `MainApp.java` → Run.

### 4. Login Default
```
Username : admin
Password : admin123
```

---

## Struktur Proyek
```
count-epiphany/
├── sql/
│   └── schema.sql              ← Script database lengkap + triggers
├── src/main/
│   ├── java/com/countepiphany/
│   │   ├── MainApp.java        ← Entry point JavaFX
│   │   ├── model/              ← 7 Model class (POJO)
│   │   ├── dao/                ← 6 DAO class (JDBC)
│   │   ├── service/            ← 7 Service class (Business Logic)
│   │   ├── controller/         ← 7 Controller class (JavaFX)
│   │   └── util/               ← Helper: DB, Session, Alert, Currency
│   └── resources/
│       ├── database.properties ← Konfigurasi DB
│       └── com/countepiphany/
│           ├── css/style.css   ← Global stylesheet (tema ungu)
│           └── fxml/           ← 7 file FXML (UI per halaman)
└── pom.xml
```

## Arsitektur
```
Presentation Layer  →  FXML + Controller (JavaFX)
       ↕
Business Logic Layer →  Service classes
       ↕
Data Access Layer   →  DAO classes (JDBC)
       ↕
Database            →  MySQL (count_epiphany)
```

## Fitur Lengkap
- **Login** dengan BCrypt password hashing
- **Transaksi Penjualan** — keranjang, diskon, cashless/tunai, cetak struk PDF
- **Manajemen Inventori** — CRUD barang, peringatan stok rendah
- **Manajemen Supplier** — CRUD supplier terhubung ke pembelian
- **Pembelian/Restock** — satu-satunya jalur tambah stok (via trigger DB)
- **Laporan Keuangan** — per periode, export PDF & Excel
- **Riwayat Transaksi** — drill-down ke detail per transaksi

## Catatan Penting
- Stok barang **hanya** bertambah melalui fitur Pembelian (trigger database)
- Password baru bisa di-hash menggunakan `AuthService.hashPassword("password")`
- Ubah `NAMA_TOKO` di `StrukService.java` dan `LaporanService.java` sesuai nama toko
