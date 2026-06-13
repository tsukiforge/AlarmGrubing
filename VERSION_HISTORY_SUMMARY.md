# Ringkasan Riwayat Versi Aplikasi (Version History Summary) — Alarm Grup
*Format ringkasan rilis ini dibuat ramah copy-paste untuk mempermudah pembaruan di konsol pengembang **APKPure Developer** maupun **Google Play Console**.*

---

## 🚀 Versi Rilis Terbaru (Stabil): **v1.1.0**
> **Arsitektur Rilis**: Lompat dari rilis debug `v1.0.28` langsung menuju rilis stabil komersial `v1.1.0` guna memperkenalkan fitur Mayor "Transfer & Berbagi Berkas".

### **Apa yang Baru di Versi v1.1.0? (Copy-Paste untuk APKPure / Play Store)**
* **📁 Fitur Transfer & Berbagi Berkas Asimetris**:
  * Mengirim dan menerima file nada dering kustom, audio (.mp3, .wav), dokumen, atau gambar belajar antar-perangkat secara instan melalui server cloud.
  * **Tanpa Perlu Login/Daftar**: Keamanan asimetris berbasis Kode PIN 6-digit acak sekali pakai yang aman dan bersih.
  * **Optimasi Ukuran Berkas**: Didukung unggahan aman dengan enkripsi kompresi memori murni dan limitasi maksimal 35 MB demi menghemat resource RAM dan daya baterai.
* **📂 Dasbor Riwayat Berkas Lokal**:
  * Menghadirkan daftar "Riwayat Berkas Saya" yang terintegrasi langsung di tab penyimpanan lokal perangkat.
  * Dilengkapi aksi langsung: Bagikan ke aplikasi luar (Share Sheets), Kirim Ulang Instan dalam 1-ketukan, dan Hapus Permanen.
* **🌸 Survei Berlangganan & Dukungan**:
  * Dialog responsif otomatis setelah pengiriman/penerimaan berhasil untuk mengumpulkan umpan balik pengguna terkait survei donasi sukarela demi kelangsungan fitur bebas iklan di [Takesurvey.html](https://faizinuha.github.io/AlarmGrubing/Takesurvey.html).
* **⚙️ Kompatibilitas FileProvider Android**:
  * Sistem `FileProvider` bertipe aman untuk memproses pembagian file lokal ke aplikasi luar secara mulus dan mematuhi regulasi keamanan Android terbaru.

---

## 📜 Riwayat Update Sebelumnya (Sesuai Log Rilis GitHub)

### 🌸 **Versi v1.0.20 s.d v1.0.28 (Release Candidate & Bug Fixes)**
* **v1.0.28**: Sinkronisasi instan & perbaikan layout widget memo sakura desktop.
* **v1.0.27**: Optimasi memori render file gambar profil privat agar terhindar dari lag.
* **v1.0.26**: Penyesuaian tema visual aksen gelap metalik onyx dan kontras teks accessibility.
* **v1.0.25**: Integrasi widget homescreen "Catatan Sakura 🌸" interaktif (opsi grid 2x2 dan 4x2).
* **v1.0.24**: Perbaikan penanganan delay bunyi alarm akibat fitur Doze Mode baterai OS Android.
* **v1.0.23**: Penambahan setting alternatif DNS Pribadi (Google/Cloudflare) tanpa bantuan VPN.
* **v1.0.21**: Optimalisasi Firebase DB client untuk kelancaran sinkronisasi kamar tanpa blokir ISP.
* **v1.0.20**: Redesain visual holistik bertema Pastel Sakura Kawaii beserta Mascot Cat ekspresif peneman tidur.

### 👥 **Versi v1.0.7 s.d v1.0.8 (Alpha & Awal Sinkronisasi Cloud)**
* **v1.0.8**: Implementasi database internal luring berbasis SQLite Room Database (Offline-First).
* **v1.0.7**: Sinkronisasi real-time grup asinkron dan fungsionalitas autogenerasi token kamar kelompok.

### ⏰ **Versi v1.0.3 s.d v1.0.6 (Prototipe & Core Engine)**
* **v1.0.6**: Pemisahan antarmuka kerja menjadi Tiga Tab Utama (Pribadi, Grup, dan Catatan).
* **v1.0.5**: Pengenalan setelan loop volume bunyi bertahap dan pemutar musik nada kustom lokal.
* **v1.0.4**: Implementasi scheduler sistem yang tangguh mengadopsi native Android `AlarmManager`.
* **v1.0.3**: Inisiasi awal platform serta arsitektur Clean Architecture Model-View-ViewModel (MVVM).
