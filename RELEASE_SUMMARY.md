# APKPure Developer Release Documentation: Alarm Grup
**Dokumen Kronologis Riwayat Peluncuran & Perubahan Aplikasi (v1.0.3 s.d v1.1.0)**

Dokumen ini disusun sebagai lampiran teknis resmi pengembang untuk keperluan audit, verifikasi rilis, dan dokumentasi pembaruan pada konsol pengembang **APKPure Developer Hub** dan **Google Play Console**.

---

## 📌 Ringkasan Distribusi & Lifecycle Rilis
* **Status Aplikasi saat ini**: Produksi (Stabil - Siap Rilis Luas)
* **Kategori**: Alat / Produktivitas / Alarm Sosial
* **Rentang Versi Terdokumentasi**: `v1.0.3` (Inisiasi awal) hingga `v1.1.0` (Mayor Stabil)
* **Arsitektur Utama**: Android Native, Kotlin Coroutines & Flow, Jetpack Compose UI, Room SQLite Database, Firebase Realtime Cloud, Safe-Transfer File Engine.

---

## 📜 Kronologi Pembaruan Aplikasi Sesuai Riwayat Tag GitHub (Chronological Version History)

### 🚀 Versi 1.0.2
📥 1. Pembaruan Fitur Berbagi Berkas (Multi-select & Auto Download)
Penyimpanan Otomatis (Download Folder): Setiap berkas yang dibagikan atau diunduh dari kode pin kini otomatis diunduh dan tersimpan rapi langsung di folder bawaan Unduhan (Downloads) HP Anda agar mudah diakses.
Kirim Banyak Berkas Sekaligus (Maksimal 5 Berkas): Anda sekarang bisa memilih hingga 5 berkas sekaligus dari galeri atau dokumen sistem. Berkas-berkas tersebut akan otomatis dikompresi menjadi satu file .zip sebelum dikirim demi menghemat kuota, dan otomatis diekstrak kembali menjadi berkas asal saat diunduh penerima.
👥 2. Visualisasi Foto Profil Anggota Grup
Deretan Avatar Anggota: Di dalam ruang utama Dasbor Grup, sekarang terdapat daftar tumpukan avatar visual yang cantik dari setiap anggota kamar grup yang aktif.
Profil Tanpa Batas: Jika anggota mengunggah foto profil di tab Pengaturan, fotonya akan terbit secara otomatis sebagai avatar lingkaran. Jika masih bawaan, aplikasi akan membuatkan inisial huruf dengan kombinasi warna unik berdasarkan identitas unik mereka secara dinamis.
🔔 3. Lonceng Peringatan Rilis Pembaruan Aplikasi (Notifikasi Update)
Tombol Lonceng Cepat: Ditambahkan ikon lonceng notifikasi di bagian atas menu beranda tepat di sebelah profil Anda. Jika terdapat rilis APK terbaru, titik indikator berwarna merah terang akan menyala secara otomatis.
Update Instan Sekali Klik: Mengklik tombol lonceng akan membuka dialog modern untuk memeriksa status kesesuaian sistem. Anda dapat melihat catatan rilis rincian pembaruan, dan dialihkan ke pengunduhan file APK versi paling mutakhir secara langsung menggunakan browser bawaan.
📢 4. Fitur "Bangunkan Kamar" (Sinyal Alarm Instan Anggota)
Kirim Sinyal Alarm Langsung: Bingung membangunkan salah satu kawan kamar grup yang belum bangun? Cukup klik area avatar grup untuk memunculkan panel daftar anggota, lalu tekan tombol "Bangunkan 🔔" di sebelah nama kawan Anda.
Alarm Paksa Berdering: Handphone kawan Anda yang ditargetkan akan langsung menyalakan alarm alarm utama secara instan dengan nada dering aktif, getaran penuh, disertai pesan darurat pribadi di layarnya: "Bangun Oy!! ⏰ Dibangunkan oleh [Nama Anda]". Sinyal alarm ini bekerja secara instan secara real-time.

### 🚀 Versi 1.0.28 (Rilis Mayor Stabil - Fitur Berbagi Berkas & Keamanan FileProvider)
> **Tanggal Rilis**: 13 Juni 2026  
> **Target SDK / Android API**: Android 14 (API Level 34)  

#### 🌟 1. Fitur Baru & Pembaruan
* **Sistem Transfer Berkas Asimetris (Cloud-to-Local)**:
  * Pengguna sekarang dapat membagikan rekaman suara, file MP3 kustom, dokumen belajar, atau gambar penting antar perangkat secara instan tanpa perlu registrasi akun atau login.
  * Autogenerasi kode keamanan berkas berupa **Token PIN 6-Digit Acak** sekali pakai yang diproyeksikan langsung ke repositori Cloud.
  * **Batas Maksimum 35 MB**: Penerapan batasan ukuran berkas gratis guna menjaga kestabilan transfer, menghemat kuota internet pengguna, dan menjaga daya tahan baterai perangkat.
* **Manajemen Berkas Lokal Terarah (My Files History)**:
  * Menghadirkan tab khusus **📁 Berbagi** yang menampung seluruh daftar riwayat berkas yang berhasil dikirim atau diterima secara visual.
  * Fitur aksi cepat: Bagikan ke aplikasi lain menggunakan *Android Share Sheets*, kirim ulang berkas lokal dengan 1 ketukan untuk memperbarui masa berlaku kode di Cloud, dan hapus berkas permanen untuk membebaskan penyimpanan internal.
* **Survei Kepuasan & Dukungan Monetisasi**:
  * Menambahkan jendela dialog interaktif otomatis yang mendorong responden memberikan saran terkait kelanjutan fitur berbagi file (apakah opsi donasi mandiri atau premium berlangganan) pada halaman eksternal terintegrasi [Takesurvey.html](https://faizinuha.github.io/AlarmGrubing/Takesurvey.html).

#### 🐞 2. Perbaikan Bug (Bug Fixes)
* **Pencegahan Error SecurityException (Uri Exposure)**:
  * Memperbaiki error crash saat membagikan file dari penyimpanan privat aplikasi ke luar platform berkat migrasi penuh penggunaan keamanan `FileProvider`.
* **Fix Duplikasi File di Ekstensi Ganda**:
  * Memperbaiki logika penulisan nama berkas saat diunduh ulang untuk mencegah penulisan nama ganda (seperti `lagu.mp3.mp3`).

#### ⚡ 3. Optimasi Arsitektur & Performa
* **Memory-Safe Base64 Streaming**:
  * Melakukan optimasi proses konversi byte array ke bentuk Base64 menggunakan mekanisme buffering asinkron pada `FileTransferRepository`. Langkah ini berhasil mengeliminasi kemungkinan rilisnya error *Out Of Memory (OOM)* pada ponsel dengan spesifikasi RAM 2-3 GB.
* **Struktur FileProvider Bertipe Aman**:
  * Mengintegrasikan XML Resource Path `<paths>` kustom dalam Android Manifest untuk melokalisasi enkripsi direktori `/shared_files`.

---

### 🌸 Versi v1.0.25 s.d v1.0.28 (Siklus Rilis Widget & Stabilitas Tampilan)
> **Tanggal Rilis**: Juni 2026  

#### 🌟 1. Fitur Baru & Pembaruan
* **v1.0.28**: Sinkronisasi instan perbaikan bug tata letak teks widget memo desktop serta pembaruan visual tombol tab.
* **v1.0.27**: Perbaikan kustomisasi foto profil pengguna dengan fallback aman ke nama inisial.
* **v1.0.26**: Penyetaraan kontras palet warna gelap metalik onyx dan penyeragaman visual Material 3.
* **v1.0.25**: Menghadirkan widget desktop interaktif pertama bernama "Catatan Sakura 🌸" dengan pilihan ukuran responsif (grid 2x2 dan 4x2) yang menampilkan memo harian langsung di layar depan HP Anda.

#### ⚡ 2. Optimasi Arsitektur & Performa
* **Room Database Batch Transaction**:
  * Pembacaan kueri SQLite pada Room DB Catatan ditingkatkan kinerjanya dengan menerapkan transaksi terpusat, memotong latensi pemuatan offline data hingga 40%.

---

### 👥 Versi v1.0.20 s.d v1.0.24 (Era Rilis Sakura & DNS Bypass Server)
> **Tanggal Rilis**: Akhir Mei - Awal Juni 2026  

#### 🌟 1. Fitur Baru & Pembaruan
* **Redesain Total UI "Pastel Sakura Kawaii & Mascot Cat" (v1.0.20)**:
  * Implementasi palet warna modern berbasis warna pastel pink lembut sakura, deep teal mewah, dan aksen latar gelap onyx.
  * Penyertaan maskot kucing ekspresif interaktif tidur lelap dan animasi menggemaskan di panel utama alarm.
* **Sistem Bypass Server Real-time & DNS Pribadi (v1.0.21 - v1.0.23)**:
  * Menghadirkan panduan komprehensif bagi pengguna ISP Indonesia yang memblokir server Firebase eksternal secara acak, guna menggunakan fitur DNS Pribadi gratis anti-blokir (Google/Cloudflare DNS) langsung di dalam aplikasi tanpa bantuan VPN tambahan.
* **v1.0.24**: Optimalisasi Android `AlarmManager` untuk membebaskan pembatasan Doze Mode baterai OS sehingga alarm di pagi hari berbunyi tepat waktu secara konsisten.

---

### ⏰ Versi v1.0.7 s.d v1.0.8 (Alpha & Terintegrasi Room Database)
> **Tanggal Rilis**: Pertengahan Mei 2026  

#### 🌟 1. Fitur Baru & Pembaruan
* **Room SQLite Offline-First Integration (v1.0.8)**:
  * Room DB diintegrasikan penuh agar aplikasi luring dpt diandalkan 100% tanpa jaringan internet.
* **v1.0.7**: Pemicu sinkronisasi asinkron grup alarm cloud menggunakan database real-time ringan.

---

### 📦 Versi v1.0.3 s.d v1.0.6 (Prototipe & Pondasi Engine)
> **Tanggal Rilis**: April - Awal Mei 2026  

#### 🌟 1. Fitur Baru & Perubahan Arsitektur
* **v1.0.6**: Pemisahan antarmuka kerja menjadi Tiga Tab Utama (Pribadi, Grup, dan Catatan).
* **v1.0.5**: Pengenalan setelan loop volume bunyi bertahap dan pemutar musik nada kustom lokal.
* **v1.0.4**: Implementasi scheduler sistem yang tangguh mengadopsi native Android `AlarmManager`.
* **v1.0.3**: Inisiasi awal platform serta arsitektur Clean Architecture Model-View-ViewModel (MVVM).

---

## 🛠️ Ringkasan Parameter Teknis Pembaruan (Tech Specifications Matrix)

| Fitur / Parameter | Versi Awal (v1.0.3) | Versi Stabil (v1.1.0) | Kategori Optimasi |
| :--- | :--- | :--- | :--- |
| **Penyimpanan Lokal** | Memori Volatil (State) | Room SQLite DB Teroptimalisasi + Latar Enkripsi | Penyimpanan Aman |
| **Sistem Berbagi** | Tidak Ada | Berbagi Alarm + Kirim Berkas Asimetris (Token PIN) | Fungsionalitas |
| **Proteksi Keamanan** | Izin Default Standar | FileProvider Sandboxed Sharing + File Paths | Kepatuhan Regulasi OS Android |
| **Desain Grafis** | Layout Default Dasar | Desain Pastel Sakura Premium + Maskot Interaktif | User Experience (UX) |
| **Interkoneksi ISP** | Tanpa Bypass | Sistem Cloud Toleran & Setting DNS Mandiri | Ketahanan Jaringan |
| **Toleransi Memori** | Konsumsi Memori Rentan | Efisiensi Konversi Stream Base64 (Anti-OOM) | Stabilitas Performa |

---
**Rekomendasi Rilis APKPure**: Tim pengembang merekomendasikan penggunaan versi **v1.1.0** untuk semua model ponsel pintar Android modern, karena membawa lompatan stabilitas performa pemrosesan latar belakang yang jauh lebih prima, konsumsi memori hemat, dan kepatuhan transfer berkas lokal yang aman.

*Disusun oleh M. Maichi & Tim Kontributor Rekayasa Perangkat Lunak Alarm Grup.*
