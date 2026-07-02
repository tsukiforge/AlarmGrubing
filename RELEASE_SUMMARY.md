# 📌 Ringkasan Rilis (Release Summary) - Alarm Grup

Dokumen resmi peninjauan rilis produk dan kronologi siklus hidup paket distribusi **Alarm Grup** untuk rilis stabil **v5.2.0**.

---

## 📦 Informasi Metadata Distribusi
* **Versi Produksi Aktif**: `v5.2.0`
* **Tanggal Rilis**: 2 Juli 2026
* **Arsitektur Rilis**: Kompilasi multi-arsitektur CPU (ARM64-v8a, ARMEABI-v7a, Universal).
* **Lingkungan Target**: Kompatibel dari Android 7.0 (API Level 24) hingga Android 16+ (API Level 36+).

---

## 🌟 Sorotan Riwayat Fitur Berdasarkan Kronologi Versi

### 🟣 Versi v5.2.0 (Rilis Stabil Terbaru)
* **Wallpaper AOD Animasi Bergerak**: Menambahkan dukungan video animasi sebagai latar belakang AOD (muted, looping, alpha 25%). File .mp4 diintegrasikan via VideoView dalam Jetpack Compose.
* **Penyederhanaan Template AOD**: Template AOD dirampingkan dari 9 menjadi 3 template bawaan statis. Template ekstra anime/gambar tidak diperlukan dihapus.
* **Pembersihan Aset Global**: 3 file drawable tidak terpakai dihapus. Folder `drawable/Aod/` dibersihkan dan file .mp4 dipindahkan ke `res/raw/` dengan penamaan standar.
* **Persiapan Distribusi Store**: Penambahan file `STORE_METADATA.md` lengkap, update `metadata.json`, dan pembuatan screenshot test otomatis dengan Roborazzi untuk Play Store & APKPure.
* **Perbaikan App Name**: Nama aplikasi diperbarui menjadi "Alarm Sync — Alarm Grup".

### 🟢 Versi v5.1.0
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
