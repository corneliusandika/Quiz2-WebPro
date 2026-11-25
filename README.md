# Dompetku JSP

Ini proyek Exam 2 Web Programming D. Dompetku sebelumnya saya buat dengan Laravel, lalu saya tulis ulang memakai JSP, Servlet, dan JDBC supaya bisa berjalan di Tomcat dengan MySQL.

Saya mempertahankan alur utama aplikasi: mencatat transaksi, memantau budget, menabung untuk target, dan melihat laporan. Versi ini sudah saya jalankan di lokal dan saya coba deploy ke Azure Web App.

**Cornelius Andika** · 5025241123

## Fitur

- Registrasi, login, logout, dan reset password lewat token.
- Dashboard berisi saldo, pemasukan/pengeluaran, arus kas, budget, target, dan jatuh tempo.
- CRUD transaksi dengan filter bulan, jenis, kategori, dompet, pencarian, dan pagination.
- CRUD kategori dan dompet. Saldo dompet dihitung dari saldo awal ditambah transaksi.
- Budget bulanan per kategori, target tabungan, setoran tabungan, utang/piutang, dan transaksi berulang.
- Import CSV dengan pemetaan kolom, pratinjau, validasi, dan deteksi duplikat.
- Laporan CSV/PDF, sampah transaksi dengan undo, dan riwayat aktivitas.
- Profil pengguna, ganti password, dan mode gelap.

## Stack

- JSP 2.3, JSTL 1.2, Servlet 3.1 (`javax.servlet`)
- JDBC dan MySQL Connector/J 8.0.33
- Tomcat JDBC Pool
- MySQL/MariaDB dari XAMPP untuk pengembangan
- Maven untuk build WAR
- Chart.js, PDFBox, dan JavaMail

Bytecode Java 8 saya pertahankan supaya tetap kompatibel dengan Tomcat 8.5 di XAMPP. Untuk deployment, saya memakai Tomcat 9 dan Java 17.

## Menjalankan di lokal

Prasyarat:

1. XAMPP dengan MySQL dan Tomcat 8.5.
2. JDK sesuai path di `build.bat`.
3. JRE 8 sesuai path di `start.bat` dan `stop.bat`.

Cara cepat:

```bat
build.bat
start.bat
```

Lalu buka `http://localhost:8080/dompetku/`.

Akun demo lokal:

```text
Email: demo@dompetku.test
Password: password
```

Database dibuat otomatis saat aplikasi pertama kali dijalankan. Listener akan membuat database `dompetku`, menjalankan `schema.sql`, lalu mengisi data demo kalau tabel `users` masih kosong.

Kalau ingin build lewat Maven:

```bat
mvn test
mvn clean package
copy target\dompetku.war C:\xampp\tomcat\webapps\
```

## Konfigurasi database

Default lokal memakai `127.0.0.1:3306`, user `root`, password kosong, dan database `dompetku`. Nilai ini bisa diubah di `src/main/resources/db.properties` atau lewat environment variable:

```text
DOMPETKU_DB_HOST
DOMPETKU_DB_PORT
DOMPETKU_DB_NAME
DOMPETKU_DB_USER
DOMPETKU_DB_PASSWORD
DOMPETKU_DB_SSL_MODE
```

Untuk database remote, gunakan `DOMPETKU_DB_SSL_MODE=VERIFY_IDENTITY`. Mode `DISABLED` hanya untuk MySQL lokal.

Format nominal bisa diatur lewat `DOMPETKU_CURRENCY_DECIMALS`: `auto` (default), `always`, atau `never`.

## Deploy

Untuk production, jangan memakai `start.bat` karena script tersebut mengaktifkan data demo. Deploy WAR ke Tomcat 9/Java 17, lalu set minimal:

```text
DOMPETKU_ENV=production
DOMPETKU_PUBLIC_URL=https://domain-kamu
DOMPETKU_SECURE_COOKIES=true
DOMPETKU_DB_HOST=...
DOMPETKU_DB_USER=...
DOMPETKU_DB_PASSWORD=...
DOMPETKU_DB_SSL_MODE=VERIFY_IDENTITY
DOMPETKU_DB_CREATE_DATABASE=false
```

Saat `DOMPETKU_ENV=production`, aplikasi menolak start kalau URL bukan HTTPS, secure cookie belum aktif, database memakai user `root` atau password kosong, SSL database masih `DISABLED`/`PREFERRED`, verifikasi email aktif tanpa SMTP, atau akun demo masih ada.

Health check ada di:

```text
GET /health
```

Responsnya `200 {"status":"ok","database":"ok"}` kalau database bisa dihubungi.

Saat ini saya menjalankan versi Azure di Web App Tomcat 9/Java 17 dengan MySQL Flexible Server. Deployment-nya masih manual; workflow GitHub Actions belum saya pindahkan ke repo ini.

## Keamanan

- Password disimpan dengan PBKDF2-HMAC-SHA256.
- Semua POST memakai CSRF token.
- Data di JSP di-escape, dan query data dibatasi `user_id` dari session.
- Cookie session memakai `HttpOnly`, `SameSite=Lax`, dan rotasi saat login.
- Login, registrasi, dan reset password memiliki rate limit.
- Ada header keamanan dasar seperti CSP, `X-Frame-Options`, dan `X-Content-Type-Options`.

## Yang belum selesai

- SMTP belum dikonfigurasi, jadi fitur reset password dan verifikasi email belum aktif sepenuhnya.
- Rate limit masih disimpan di memori. Kalau aplikasi berjalan di lebih dari satu instance, batasnya tidak konsisten.
- Workflow CI/CD masih ada di repo induk dan belum saya rapikan untuk repo ini.
- Tomcat 10+ belum didukung karena aplikasi masih memakai `javax.servlet`.

## Struktur

```text
src/main/java/com/dompetku/   DAO, servlet, service, util
src/main/resources/           db.properties, schema.sql, seed.sql
webapp/WEB-INF/views/         halaman JSP
webapp/WEB-INF/tags/          komponen ikon
webapp/assets/                CSS, JS, Chart.js
database/                     schema dan seed
report/                       laporan dan screenshot
```

## Catatan pengerjaan

Beberapa bagian yang sempat saya perbaiki:

- Parser nominal dibuat konsisten untuk format `1500.50`, `1.500,50`, dan `1,500.50`.
- Import CSV dijalankan dalam satu transaksi dan tidak lagi memasukkan baris duplikat.
- Perubahan password mencabut session lain lewat `session_version`.
- Export laporan membaca data per batch supaya tidak memuat seluruh transaksi sekaligus.
- Logout diubah menjadi POST dengan CSRF, bukan link GET.

## Laporan

Laporan ada di `report/report.html`, `report/Dompetku-JSP-Report.pdf`, dan `report/screenshots/`.

Untuk membuat ulang screenshot dan PDF, jalankan aplikasi di port 8080 lalu:

```bat
cd tools
npm install
npm run screenshots
npm run report
```
