# 📜 Catatan Perubahan (Changelog) - Alarm Grup

Semua riwayat perubahan, rilis, perbaikan bug, dan optimasi arsitektur aplikasi **Alarm Grup** dari versi dasar hingga versi stabil terbaru.

---

## [1.1.50] - 2026-06-21

### 🚀 Fitur Baru & Peningkatan
* **Keandalan Latar Belakang (Doze Mode Bypass)**: Mengintegrasikan `PowerManager.PARTIAL_WAKE_LOCK` dengan pelepasan otomatis untuk menjamin alarm tetap berbunyi presisi dan tepat waktu saat perangkat dalam masa tidur nyenyak (*Doze Mode*) atau mode penghemat daya ekstrim.
* **Simulasi Couple Sync Interaktif**: Menyediakan panel kontrol simulasi mandiri di tab kamar alarm grup untuk menguji dinamika tidur/bangun karakter "Leon" dan "Mia" serta perhitungan bonus poin (+15 poin).
* **Personalisasi Nada Dering**: Memperbarui teks sambutan layar penuh alarm dengan sapaan hangat: `"bangunn sayanggg Miaw~ ✨"`.
* **Optimasi Ukuran Paket (ABI Splits)**: Mengonfigurasi mekanisme pemisahan otomatis paket instalasi di `build.gradle.kts` agar menghasilkan binary yang dioptimalkan khusus untuk arsitektur CPU perangkat cerdas:
  * `arm64-v8a`: Performa prima untuk ponsel Android modern 64-bit.
  * `armeabi-v7a`: Kompatibilitas optimal untuk ponsel Android 32-bit.
  * `universal`: Paket gabungan all-in-one untuk seluruh arsitektur.

### 🛠️ Perbaikan Bug
* **Sistem Update Otomatis**: Memperbaiki algoritma perbandingan versi mikro tak-simetris melalui fungsi `canonicalizeVersion` di dalam `GithubUpdateChecker` guna kelancaran deteksi update rilis.
* **Responsivitas Landing Page**: Melengkapi landing page (`index.html`) dengan helper skrip kalkulasi versi pemasaran otomatis berdasarkan patch aktif.
* **Kompabilitas Skrip CI/CD (Restore standard APK filenames)**: Menambahkan kustom task Gradle `CopyApkTask` yang ramah *Configuration Cache* untuk secara otomatis menggandakan dan menyediakan file `app-debug.apk` dan `app-release.apk` dari build universal, sehingga skrip Runner/Publishing tidak mengalami error pencarian file.

---

## [1.1.34] - 2026-06-15

### 🚀 Fitur Baru
* **Modul Couple Sync**: Sinkronisasi status alarm multi-pengguna secara real-time.
* **QR Room Sharing**: Membagikan akses grup kamar langsung menggunakan kode QR dinamis.
* **Penyimpanan Lokal Room**: Migrasi penuh penyimpanan lokal ke Room Database SQLite.

---

## [1.1.0] - 2026-05-20

### 🚀 Peningkatan Layanan
* **Cloud Sync Integration**: Sinkronisasi basis data online real-time untuk koordinasi alarm lintas perangkat.
* **UI Refresh**: Penyegaran elemen Material 3 dengan tatanan visual yang lebih kontras dan dinamis.

---

## [1.0.24] - 2026-04-12

### 🛠️ Perbaikan Bug
* **Sistem Ekstraksi ZIP**: Penanganan kompresi multi-berkas sebelum ditransmisikan agar lebih hemat kuota data internet.
* **Izin Runtime Android**: Memperketat penanganan permintaan izin akses media di Android 13+.

---

## [1.0.4] - 2026-03-05

### 🛠️ Perbaikan Bug
* **Koreksi API Handler**: Hotfix penanganan kesalahan tautan rilis tidak ditemukan saat memeriksa versi terbaru dari repositori online.

---

## [1.0.3] - 2026-02-18

### 🚀 Rilis Awal
* Standardisasi antarmuka dasar jam alarm mandiri.
* Fitur pengaturan waktu dasar dan getaran kustom.
