# 🕒 Riwayat Ringkas Versi - Alarm Grup

Peta jalan sejarah versi aplikasi **Alarm Grup** dikurasi untuk pelacakan performa dari fase rilis awal hingga rilis produksi multi-arsitektur termutakhir.

---

### 🟣 v5.1.0 (Produksi Stabil Terbaru - 2026-06-30)
* **Always-On Display (AOD)**: Implementasi background `AodService` untuk mendeteksi screen lock/off dan meluncurkan layar standby AOD secara instan (lengkap dengan pintasan Switch On/Off di halaman Pengaturan & Profil).
* **Fitur Cerdas**: Perlindungan Anti Burn-in (posisi jam dinamis), kata motivasi berganti berkala, dan penambahan wallpaper kustom galeri.
* **Widget Quick Share**: Penyusutan menu utama dengan memindahkan fungsionalitas Quick Share menjadi Widget Android murni.
* **Navigasi Ramping**: Pembaruan spasi navigasi bawah yang lega dan ergonomis.

### 🟢 v1.1.50 (2026-06-24)
* **Member Wakeup & UI**: Tombol Bangunkan menampilkan semua anggota kamar dengan limitasi getar maksimal 2x sehari serta penyederhanaan indikator status dialog kamar.
* **Background Engine**: Integrasi partial WakeLock dengan batas perlindungan otomatis 10 menit.
* **Interactive Simulation**: Panel simulasi Couple Sync ("Leon & Mia") pada beranda utama.
* **Micro-Architectures Support (ARM CPU Splits)**: Dukungan pemisahan otomatis berkas instalasi untuk chipset **ARM64-v8a** dan **ARMEABI-v7a** guna mengoptimalkan ukuran unduhan ponsel pengguna.
* **Hotfixes**: Penanganan kanonisasi penulisan versi mikro asimetris di `GithubUpdateChecker`.

### 🔵 v1.1.34 (Juni 2026)
* **Group Alarms**: Peluncuran awal Room Sync dan sistem poin Couple Sync.
* **Offline Storage**: Adopsi SQLite Room DB untuk persistensi data alarm lokal secara terjadwal.

### 🟡 v1.1.0 (Mei 2026)
* **Realtime Cloud**: Penghubung status alarm berbasis Firebase Cloud Realtime DB.
* **Material Design**: Penyempurnaan tatanan letak antarmuka sesuai panduan Material 3.

### 🟠 v1.0.3 s.d v1.0.24 (Februari - April 2026)
* **Fase Dasar**: Inisiasi aplikasi jam alarms dasar, kompresi berkas instalasi, dan perbaikan penanganan kesalahan API.
