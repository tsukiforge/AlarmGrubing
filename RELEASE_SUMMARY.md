# 📌 Ringkasan Rilis (Release Summary) - Alarm Grup

Dokumen resmi peninjauan rilis produk dan kronologi siklus hidup paket distribusi **Alarm Grup** untuk rilis stabil **v1.1.50**.

---

## 📦 Informasi Metadata Distribusi
* **Versi Produksi Aktif**: `v1.1.50`
* **Arsitektur Rilis**: Kompilasi multi-arsitektur CPU (ARM64-v8a, ARMEABI-v7a, Universal).
* **Lingkungan Target**: Kompatibel dari Android 8.0 (API Level 26) hingga Android 14+ (API Level 34+).

---

## 🌟 Sorotan Riwayat Fitur Berdasarkan Kronologi Versi

### 🟢 Versi v1.1.50 (Rilis Stabil Terbaru)
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
