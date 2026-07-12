# 📌 Ringkasan Rilis (Release Summary) - Alarm Grup

Dokumen resmi peninjauan rilis produk dan kronologi siklus hidup paket distribusi **Alarm Grup** untuk rilis stabil **v5.1.0**.

---

## 📦 Informasi Metadata Distribusi
* **Versi Produksi Aktif**: `v5.1.0`
* **Arsitektur Rilis**: Kompilasi multi-arsitektur CPU (ARM64-v8a, ARMEABI-v7a, Universal).
* **Lingkungan Target**: Kompatibel dari Android 8.0 (API Level 26) hingga Android 14+ (API Level 34+).

---

## 🌟 Sorotan Riwayat Fitur Berdasarkan Kronologi Versi

### 🟣 Versi v5.4.0 (Rilis Stabil Terbaru - 2026-07-12)
* **Health Social Enforcement (Penguncian Aplikasi Berjadwal)**: Sistem penguncian aplikasi berbasis jadwal produktif dengan monitoring real-time via foreground service, overlay kunci full-screen (WindowManager TYPE_APPLICATION_OVERLAY), verifikasi PIN, dan AlarmManager scheduler.
* **Quick Settings Tile**: Tile Android untuk mengunci aplikasi langsung dari panel notifikasi cepat, mendeteksi foreground app dan memicu overlay yang sama.
* **Material 3 TimePicker**: Peningkatan UX pemilihan waktu jadwal dengan dial mode TimePicker bawaan Compose Material3.
* **Locale Persistence**: Custom Application class dengan attachBaseContext() untuk persistensi bahasa antar sesi aplikasi.
* **Perbaikan UI Snooze**: Area swipe snooze tanpa card visual, full-width, bisa swipe di area luas.
* **Pencegahan Double Alarm**: Guard di AlarmRingingService untuk mencegah alarm berbunyi dua kali.
* **Optimasi Freeze/Lag**: Perbaikan multiple concurrent monitoring loop, ganti queryUsageStats berat ke queryEvents ringan, overlay pindah ke Main Thread, cegah re-render overlay berulang, interval polling naik ke 3 detik.
* **Penyederhanaan Bahasa**: Hanya 3 bahasa (id, en, ja) dengan default fallback ke Indonesia.

### 🟣 Versi v5.1.0 (Rilis Stabil Terbaru)
* **Always-On Display (AOD) Otomatis**: Integrasi `AodService` berbasis `BroadcastReceiver` yang mendeteksi matinya layar HP (`ACTION_SCREEN_OFF`) untuk meluncurkan layar standby AOD secara otomatis dan andal. Lengkap dengan pintasan Switch On/Off di halaman Pengaturan & Profil utama.
* **Fitur Anti-Burn-In**: Mekanisme pergeseran koordinat jam & tanggal otomatis secara periodik untuk mencegah kerusakan layar.
* **Galeri Wallpaper Kustom & Motivasi**: Mendukung pengisian gambar wallpaper kustom dari galeri pengguna serta visualisasi teks kutipan motivasi harian.
* **Widget Quick Share**: Pemindahan penuh fitur Quick Share ke dalam bentuk Widget Home Screen murni untuk membersihkan tatanan UI aplikasi utama.
* **Penyelarasan Rilis Versi**: Pembaharuan major version menjadi v5.1.0 secara presisi sesuai dengan status rilis repositori utama.

### 🟢 Versi v1.1.51
* **Fitur Bangunkan Anggota**: Tombol "Bangunkan 🔔" memunculkan seluruh anggota kamar grup dengan pembatasan (*cooldown*) global maksimal mengirim sinyal getar 2 kali sehari per pengguna.
* **Penyederhanaan UI Kamar Grup**: Tampilan kelola grup menjadi lebih bersih dan ringkas tanpa indikator warna status yang tidak perlu untuk anggota yang belum terhubung.
* **Stabilitas Versioning (CI/CD)**: Validasi pembaruan rilis OTA pada `GithubUpdateChecker` kini menggunakan standar Semantic Versioning yang akurat.
* **Background Keep-Alive**: Implementasi mekanisme `Partial WakeLock` untuk mengatasi pembatasan latar belakang OS (*Doze Mode*) guna menjamin alarm berdering tepat waktu.
* **Demonstrasi Interaktif**: Panel simulasi perilaku grup Couple Sync "Leon & Mia" pada beranda kawan tunggal.
* **Smart APK Sizing**: Integrasi ABI Splits yang memperkecil ukuran unduhan rilis APK hingga 40% berdasarkan jenis prosesor HP pengguna.

### 🔵 Versi v1.1.34
* **Kamar Kolaborasi**: Peluncuran fitur sinkronisasi grup luring dan daring (Couple Sync) menggunakan kode QR.
* **SQLite Room Persistence**: Penyimpanan data alarm lokal yang aman, terindeks, dan cepat dengan Room DB.

### 🟡 Versi v1.1.0
* **Kerangka Firebase**: Integrasi sinkronisasi cloud real-time berkelanjutan.
* **Penyesuaian Desain M3**: Penerapan warna Material 3 dinamis untuk kenyamanan mata pengguna di malam hari.

### 🟠 Versi v1.0.3 s.d v1.0.24 (Fase Inisiasi & Pembenahan Awal)
* Landasan sistem alarm lokal, kompresi berkas `.zip` serbaguna, dan perbaikan penanganan parsing versi mikro.
