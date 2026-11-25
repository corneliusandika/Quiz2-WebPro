# Dompetku — JSP Edition

Aplikasi web pencatatan keuangan pribadi yang dibangun ulang dari proyek Midterm **Dompetku** menggunakan
**Java Server Pages (JSP)**, **Servlet**, **JDBC**, dan **MySQL**. Proyek ini dibuat untuk **Exam 2**
mata kuliah **Web Programming D**.

- **Nama:** Cornelius Andika
- **NRP:** 5025241123
- **Konsep:** melanjutkan aplikasi Dompetku dari Midterm, kini dengan stack JSP (bukan Laravel).
- **URL aplikasi:** `http://localhost:8080/dompetku/`
- **Akun demo:** `demo@dompetku.test` / `password`

## Fitur

| # | Modul | Operasi |
|---|-------|---------|
| 1 | **Transaksi** | Tambah, lihat (filter bulan/jenis/kategori/dompet/kata kunci + pagination), edit, hapus |
| 2 | **Kategori** | Tambah, lihat (dengan jumlah pemakaian), edit, hapus; transaksi terkait menjadi tanpa kategori |
| 3 | **Dompet** | Tambah, lihat saldo berjalan, edit, hapus (ditolak bila masih punya transaksi) |
| 4 | **Budget** | Tambah, lihat realisasi per bulan, edit, hapus; unik per kategori + bulan |
| 5 | **Target tabungan** | Tambah, lihat progres, edit, hapus |
| 6 | **Setoran tabungan** | Tambah, lihat riwayat, edit setoran, hapus (saldo target otomatis disesuaikan) |
| 7 | **Utang & piutang** | Tambah, filter status, edit, hapus, catat pembayaran |
| 8 | **Transaksi berulang** | Tambah, lihat jadwal, edit, hapus, jeda/aktifkan, jalankan (termasuk catch-up jadwal tertunggak) |
| 9 | **Import CSV** | Upload, deteksi pemisah, pemetaan kolom, pratinjau, validasi, deteksi duplikat |
| 10 | **Profil pengguna** | Lihat profil, ubah nama/email, ganti password (verifikasi password lama) |
| 11 | **Sampah & audit** | Hapus transaksi ke sampah, urungkan, dan lihat riwayat aktivitas terakhir |
| 12 | **Reset & verifikasi** | Lupa password dan verifikasi email dengan token sekali pakai (SMTP) |

Modul inti memiliki CRUD penuh; modul import, sampah/audit, profil, dan reset password menambahkan alur khusus.
Fitur pendukung: autentikasi (registrasi, login, logout), dashboard dengan grafik, laporan bulanan,
ekspor CSV/PDF, import CSV dengan preview, soft delete + undo, audit trail, mode gelap, dan tampilan responsif.

## Teknologi

- **JSP 2.3 + JSTL 1.2** — view dan tag file untuk komponen ikon.
- **Servlet 3.1 (javax.servlet)** — controller per modul.
- **JDBC + MySQL Connector/J 8.0.33** — akses data.
- **Tomcat JDBC Pool** — connection pool (maks. 20 koneksi).
- **MariaDB/MySQL 10.4 (XAMPP)** — database.
- **Apache Tomcat 8.5** — servlet container.
- **Chart.js 4** — grafik (disimpan lokal, tanpa CDN).
- **Java 8 bytecode** — kompatibel dengan Tomcat 8.5.
- **Maven `pom.xml`** — build WAR (`mvn clean package`).
- **Apache PDFBox 2** — generator PDF laporan (Apache 2).
- **JavaMail 1.6** — email reset password/verifikasi (SMTP).
- **Docker / Tomcat 9 + Java 17** — jalur deployment production.

## Arsitektur

```
Browser
  │
  ├── JSP (WEB-INF/views)  ← JSTL, EL, tag file ikon
  ├── Servlet (@WebServlet) ← validasi input, session, flash message
  ├── DAO (JDBC)            ← query dengan batas user_id
  └── MySQL (dompetku)
```

Struktur folder:

```
dompetku-jsp/
├── src/main/java/com/dompetku/
│   ├── dao/            UserDAO, CategoryDAO, WalletDAO, TransactionDAO,
│   │                   BudgetDAO, GoalDAO, DebtDAO, RecurringDAO
│   ├── model/          POJO + computed getter (progress, status, saldo)
│   ├── servlet/        Auth, Dashboard, Transaction, Category, Wallet, Budget,
│   │                   Goal, Debt, Recurring, Report, Profile
│   ├── service/        DefaultDataService (kategori & dompet awal)
│   ├── util/           PasswordUtil (PBKDF2), Formats, Json, Tokens
│   ├── web/            BaseServlet, AppFilter (UTF-8 + CSRF), AuthFilter
│   └── db/             DB (bootstrap), AppContextListener
├── src/main/resources/
│   ├── db.properties   konfigurasi koneksi
│   └── db/             schema.sql, seed.sql
├── webapp/
│   ├── WEB-INF/views/  halaman JSP per modul + layout
│   ├── WEB-INF/tags/   icon.tag
│   ├── WEB-INF/tlds/   functions.tld
│   ├── assets/         app.css, app.js, chart.umd.min.js
│   └── index.jsp
├── database/           schema.sql, seed.sql
├── report/             report.html + PDF + screenshots
├── build.bat           compile + package WAR
├── deploy.bat          copy WAR ke Tomcat
├── start.bat           start MySQL + Tomcat + buka browser
├── stop.bat            stop Tomcat + MySQL
├── reset-db.bat        reset database demo
└── tools/              generate_seed.py
```

## Desain Database

12 tabel InnoDB dengan foreign key untuk data utama dan tabel pendukung sampah/audit:

```
users ─┬─< categories ─┬─< transactions >── wallets
       │               └─< budgets
       ├─< wallets ────────< transactions
       ├─< transactions
       ├─< transactions_archive
       ├─< audit_logs
       ├─< auth_tokens
       ├─< budgets
       ├─< goals ──────< goal_deposits
       ├─< debts
       └─< recurring_transactions >── wallets / categories
```

| Tabel | Primary key | Foreign key | Catatan |
|-------|-------------|-------------|---------|
| `users` | `id` | — | email unik, password PBKDF2, status verifikasi email |
| `categories` | `id` | `user_id → users.id` | unik (`user_id`, `name`, `type`) |
| `wallets` | `id` | `user_id → users.id` | saldo awal + transaksi = saldo berjalan |
| `transactions` | `id` | `user_id`, `wallet_id`, `category_id` | kategori `NULL` bila kategori dihapus |
| `transactions_archive` | `id` | — | salinan transaksi yang dihapus (sampah) |
| `audit_logs` | `id` | `user_id → users.id` | riwayat create/update/delete/restore/import |
| `auth_tokens` | `id` | `user_id → users.id` | token reset password / verifikasi email |
| `budgets` | `id` | `user_id`, `category_id` | unik (`user_id`, `category_id`, `month`) |
| `goals` | `id` | `user_id → users.id` | target + dana terkumpul |
| `goal_deposits` | `id` | `goal_id → goals.id` | riwayat setoran |
| `debts` | `id` | `user_id → users.id` | `payable` / `receivable` |
| `recurring_transactions` | `id` | `user_id`, `wallet_id`, `category_id` | jadwal + status aktif |

DDL lengkap: [`database/schema.sql`](database/schema.sql).

## Menjalankan Aplikasi

### Prasyarat

1. **XAMPP** terpasang di `C:\xampp` (MySQL + Tomcat 8.5).
2. **JDK** untuk kompilasi. Build script memakai `D:\code\JDK`; ubah baris `JAVA_HOME` di `build.bat`
   bila lokasi JDK berbeda.
3. **JRE 1.8** untuk menjalankan Tomcat. Script memakai `C:\Program Files\Java\jre-1.8`.
   Sesuaikan `JAVA_HOME` di `start.bat` dan `stop.bat` bila perlu.
4. Koneksi internet hanya dibutuhkan saat pertama kali build (unduh JDBC, JSTL, PDFBox, JavaMail, Chart.js).
   Setelah itu aplikasi berjalan sepenuhnya lokal.

### Cara cepat

```bat
cd dompetku-jsp
build.bat      :: kompilasi + package build\dompetku.war
start.bat      :: start MySQL, deploy WAR, start Tomcat, buka browser
```

Buka `http://localhost:8080/dompetku/`, lalu login dengan akun demo.

### Manual

```bat
build.bat
deploy.bat
:: nyalakan MySQL dari XAMPP Control Panel
:: nyalakan Tomcat dari XAMPP Control Panel
```

Database dibuat otomatis saat aplikasi pertama kali dijalankan (listener `AppContextListener`):
membuat database `dompetku`, menjalankan `schema.sql`, lalu `seed.sql` bila tabel `users` kosong.

### Build dengan Maven (opsional)

```bat
mvn test
mvn clean package
copy target\dompetku.war C:\xampp\tomcat\webapps\
```

Project memakai `javax.servlet` dan Java 8 bytecode agar kompatibel dengan Tomcat 8.5 lokal. Untuk production gunakan Tomcat 9 + Java 17 (Docker tersedia); Tomcat 10+ perlu migrasi ke Jakarta Servlet 6/JSTL 3.

### Reset data demo

```bat
reset-db.bat
```

Atau hentikan Tomcat, hapus database `dompetku`, lalu start ulang aplikasi.

### Konfigurasi database

Default (XAMPP): `127.0.0.1:3306`, user `root`, password kosong, database `dompetku`.
Ubah `src/main/resources/db.properties` atau gunakan environment variable:
`DOMPETKU_DB_HOST`, `DOMPETKU_DB_PORT`, `DOMPETKU_DB_NAME`, `DOMPETKU_DB_USER`, `DOMPETKU_DB_PASSWORD`.
Untuk database remote, set `DOMPETKU_DB_SSL_MODE=VERIFY_IDENTITY`; default `DISABLED` hanya untuk MySQL lokal.

Format tampilan nominal bisa diatur lewat `DOMPETKU_CURRENCY_DECIMALS`:
`auto` (default, desimal hanya jika ada sen), `always` (selalu dua desimal), atau `never` (tanpa desimal).

## Deployment Production

Jangan memakai `start.bat` untuk production karena script itu mengaktifkan data demo. Gunakan WAR + Tomcat 9/Java 17 (Docker tersedia) atau Azure Web App.

### Environment wajib

| Variable | Nilai production |
|----------|------------------|
| `DOMPETKU_ENV` | `production` |
| `DOMPETKU_PUBLIC_URL` | `https://domain-kamu/dompetku` (termasuk context path jika ada) |
| `DOMPETKU_SECURE_COOKIES` | `true` (hanya jika HTTPS aktif) |
| `DOMPETKU_DB_HOST/PORT/NAME/USER/PASSWORD` | kredensial database khusus aplikasi |
| `DOMPETKU_DB_SSL_MODE` | `VERIFY_IDENTITY` untuk database remote/Azure; `DISABLED` hanya untuk MySQL lokal |
| `DOMPETKU_DB_CREATE_DATABASE` | `false` bila database sudah dibuat oleh hosting dan user aplikasi tidak punya izin membuat database |
| `DOMPETKU_SEED_DEMO` | tidak diset atau `false` |
| `DOMPETKU_AUDIT_RETENTION_DAYS` | `365` (atau sesuai kebijakan) |
| `DOMPETKU_SMTP_*` | wajib jika memakai reset password/verifikasi email |
| `DOMPETKU_REQUIRE_EMAIL_VERIFICATION` | `true` hanya jika SMTP sudah teruji |

`DOMPETKU_ENV=production` memblokir demo seed dan login demo walaupun `DOMPETKU_SEED_DEMO` salah disetel. Aplikasi juga **menolak start** di production jika:

- `DOMPETKU_PUBLIC_URL` bukan HTTPS
- `DOMPETKU_SECURE_COOKIES` bukan `true`
- memakai user database `root` atau password kosong
- `DOMPETKU_DB_SSL_MODE` masih `DISABLED`/`PREFERRED`
- verifikasi email aktif tanpa SMTP
- akun `demo@dompetku.test` masih ada

Saat production, request HTTP biasa diarahkan **308** ke `DOMPETKU_PUBLIC_URL` (kecuali `/health`). Pastikan reverse proxy mengirim `X-Forwarded-Proto: https` agar tidak terjadi redirect loop.

Untuk database yang sudah dibuat oleh penyedia hosting dan user aplikasi tidak punya izin membuat database, set `DOMPETKU_DB_CREATE_DATABASE=false`.

### Hapus akun demo

Sebelum production, jalankan sekali pada database yang sudah tidak membutuhkan data demo:

```bat
mysql -u root -p dompetku < tools\remove_demo_account.sql
```

### Docker

```bat
copy .env.example .env
:: isi MYSQL_ROOT_PASSWORD, PUBLIC_URL, SMTP, dan secure cookie
docker compose up -d --build
```

Aplikasi tersedia di `http://localhost:8080/dompetku/`. Compose default memakai `DOMPETKU_ENV=development` agar bisa dicoba lewat HTTP; untuk production ubah ke `production` setelah HTTPS, secure cookie, DB TLS, dan user database dedicated terpasang. Pada production, request HTTP akan diarahkan 308 ke `DOMPETKU_PUBLIC_URL`; pastikan proxy mengirim `X-Forwarded-Proto: https` agar tidak terjadi redirect loop.

### Azure Web App

Workflow:

- `.github/workflows/dompetku-ci.yml` — build WAR dan artifact.
- `.github/workflows/dompetku-azure.yml` — deploy ke Azure Web App.

Buat App Service **Java 17 + Tomcat 9**, lalu set repository secrets:

- `AZURE_WEBAPP_NAME`
- `AZURE_WEBAPP_PUBLISH_PROFILE`

Set environment variable database, `DOMPETKU_ENV=production`, `DOMPETKU_PUBLIC_URL`, `DOMPETKU_SECURE_COOKIES=true`, dan `DOMPETKU_DB_SSL_MODE=VERIFY_IDENTITY` di App Service. Tomcat 10 belum dipakai karena aplikasi masih memakai `javax.servlet`.

### Health check

```text
GET /dompetku/health
```

Respons `200 {"status":"ok","database":"ok"}`; `503` bila database tidak dapat dihubungi. Endpoint ini tidak memerlukan login dan cocok untuk load balancer/uptime monitor.

### Backup dan restore

```powershell
powershell -ExecutionPolicy Bypass -File tools\backup_db.ps1 -KeepDays 30
```

Script membuat file `backups/dompetku-YYYYMMDD-HHMMSS.sql` menggunakan `mysqldump`. Jadwalkan harian dengan Task Scheduler. Simpan backup di storage terenkripsi dan uji restore secara berkala:

```bat
mysql -u root -p dompetku < backups\dompetku-YYYYMMDD-HHMMSS.sql
```

### Email reset password/verifikasi

Halaman `/forgot-password` mengirim tautan sekali pakai; token disimpan sebagai SHA-256 hash dan berlaku 30 menit. Verifikasi email memakai token 24 jam. Tanpa SMTP, form tetap memberi respons generik dan token tidak dikirim—konfigurasikan `DOMPETKU_SMTP_*` sebelum mengaktifkan verifikasi email.

### Audit retention

Audit lama dibersihkan saat startup berdasarkan `DOMPETKU_AUDIT_RETENTION_DAYS` (default 365 hari). Transaksi di sampah tidak dibersihkan otomatis.

## Keamanan

- Password di-hash **PBKDF2-HMAC-SHA256** (600.000 iterasi, salt 16 byte; hash lama tetap dapat diverifikasi).
- Semua POST memakai **CSRF token** per session; token divalidasi filter.
- Semua data user di JSP di-escape dengan `f:esc()`; JSON grafik meng-escape `<`, `>`, `&`.
- Semua query data dibatasi `user_id` dari session (isolasi antar akun).
- Validasi input di sisi server untuk setiap form; ownership wallet/category diperiksa.
- Session cookie `HttpOnly`, `SameSite=Lax`, timeout 8 jam, dan dirotasi saat login.
- Rate limiting login: maksimal 5 kegagalan per 15 menit per IP + email.
- Rate limiting registrasi dan permintaan reset password.
- Reset password/verifikasi email memakai token acak yang di-hash, sekali pakai, dan kedaluwarsa.
- Header `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`, `Content-Security-Policy`.
- Delete dompet ditolak bila masih memiliki transaksi; delete kategori memakai `ON DELETE SET NULL`.
- Delete pada modul lain memeriksa kepemilikan terlebih dahulu (ID user lain → 404).
- Database memiliki CHECK constraint untuk nominal positif, `paid_amount <= amount`, `saved_amount >= 0`, dan awal bulan.

## Alur Demo untuk Presentasi

1. Login dengan akun demo di lingkungan lokal.
2. Dashboard: saldo, pemasukan/pengeluaran, arus kas 6 bulan, kategori, budget, target, jatuh tempo.
3. Transaksi: filter bulan/jenis/kategori, cari, tambah, edit, hapus.
4. Kategori & Dompet: lihat komponen CRUD dan saldo berjalan.
5. Budget: progress aman/hampir habis/over.
6. Target tabungan: buka detail, catat setoran, edit setoran, lihat riwayat.
7. Utang/piutang: catat pembayaran sebagian.
8. Transaksi berulang: klik **Jalankan** untuk membuat transaksi dan memajukan jadwal.
9. Laporan: unduh CSV atau PDF.
10. Transaksi: klik **Import CSV**, upload file, periksa mapping dan pratinjau, lalu konfirmasi.
11. Hapus transaksi: gunakan tombol **Urungkan** pada banner, lalu cek **Riwayat aktivitas** di profil.
12. Profil: ubah nama/email dan ganti password.
13. Toggle mode gelap di topbar.

## Troubleshooting

| Masalah | Solusi |
|---------|--------|
| `BUILD FAILED` | Pastikan `D:\code\JDK\bin\javac.exe` ada; ubah `JAVA_HOME` di `build.bat`. |
| `Could not initialise Dompetku database` | Pastikan MySQL XAMPP berjalan (port 3306) dan kredensial benar. |
| Halaman 500 setelah deploy | Cek `C:\xampp\tomcat\logs\localhost.*.log`; pastikan WAR ter-copy dan JDBC driver ada di `WEB-INF/lib`. |
| Port 8080 dipakai | Hentikan proses lain / ubah port di `C:\xampp\tomcat\conf\server.xml`. |
| Tomcat gagal start | Pastikan `JAVA_HOME` di `start.bat` menunjuk JRE/JDK 8 yang valid. |

## Laporan

Laporan Exam 2 (desain database, daftar fitur, screenshot, penjelasan per fitur) tersedia di:

- `report/report.html`
- `report/Dompetku-JSP-Report.pdf`
- `report/screenshots/`

Untuk meregenerasi screenshot dan PDF (aplikasi harus berjalan di port 8080):

```bat
cd tools
npm install
npm run screenshots
npm run report
```

Script memakai Chrome di `C:\Program Files\Google\Chrome\Application\chrome.exe`.
Ubah konstanta `CHROME` di kedua script bila lokasi Chrome berbeda.
