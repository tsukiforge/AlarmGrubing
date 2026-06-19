# 📜 Berkas Catatan Perubahan (CHANGELOG) — Alarm Grup
Semua catatan rilis, riwayat pengembangan, dan optimasi arsitektur dari versi dasar **v1.0.3** hingga versi stabil mutakhir **v1.1.34** dirangkum secara kronologis dalam dokumen ini.

---

## 📌 Mengapa Menggunakan Versi `v1.1.34` Bukan `v1.1.3`?

Mungkin Anda bertanya mengapa versi rilis ini melompat ke angka **`1.1.34`** dan bukan format umum **`1.1.3`**. Berikut adalah penjelasan teknis mengapa skema penomoran ini sangat ideal untuk manajemen rilis profesional:

1. **Sinkronisasi Kode Build (Build/Patch Mapping)**:  
   Dalam siklus pengembangan perangkat lunak modern (Continuous Integration & Continuous Delivery / CI/CD) untuk platform Android, nomor build internal (build number) dipetakan langsung dengan patch rilis. Angka `34` menandakan bahwa rilis stabil ini telah melalui **34 kali iterasi build mikro, kompilasi uji coba otomatis, dan pengujian kualitas** di fase internal sebelum akhirnya ditandai sebagai layak rilis (Production-Ready).
   
2. **Kepatuhan Konsol Google Play & APKPure (Sequential Versioning)**:  
   Toko aplikasi seperti Google Play Store dan APKPure memerlukan `versionCode` (integer) yang **harus terus meningkat secara berurutan** di setiap unggahan berkas APK/AAB baru (misalnya dari `30` ke `34`). Menyamakan angka terakhir `versionName` (`1.1.34`) dengan `versionCode` (`34`) adalah praktik terbaik industri (*industry best practice*) agar tim pengembang dapat dengan mudah mencocokkan laporan crash, log sistem, dan masalah teknis langsung dengan versi yang diunduh pengguna di App Store.
   
3. **Pembedaan Iterasi Fitur Mayor**:  
   Secara Semantik, jika kita menggunakan rilis `v1.1.3`, itu hanya memberikan ruang untuk 9 kali revisi mikro (0-9) sebelum harus mengganti minor rilis. Dengan format desimal multi-digit (`1.1.34`), sistem memiliki fleksibilitas tinggi untuk melacak rilis hotfix yang sangat cepat di lapangan tanpa harus melompati skala rilis versi minor (`v1.2.0`).

---

## 🔄 Kronologi Riwayat Rilis Lengkap (v1.0.3 s.d v1.1.34)

### 🚀 **Versi v1.1.34 (Terbaru - Rilis Stabil Mayor)**
*Fokus Utama: Implementasi Fitur Interaksi Pasangan (Couple Sync Mode), Peningkatan Algoritma Poin, dan Widget Interaktif Mandiri.*

*   **Dashboard UI Couple Sync (MainActivity)**:
    *   Menghadirkan komponen visual interaktif bertema cinta (pink lembut) yang merefleksikan status keterhubungan dua orang.
    *   Menampilkan skor akumulasi poin individu pasangan dan jumlah streak harian berkelanjutan.
    *   Sinyal Indikator Cepat **⚡ Sync Bonus Aktif!** yang menyala secara otomatis ketika kedua pasangan berhasil bangun pagi dengan selisih waktu di bawah 10 menit (bonus tambahan +15 poin).
    *   Tombol instan **Putuskan Pasangan 💔** untuk melakukan unpair secara aman langsung di dasbor.
*   **Alur Pengelolaan Pasangan yang Aman & Fleksibel**:
    *   Tombol indikator **💕 Pair** yang diletakkan di sebelah nama anggota kamar untuk memudahkan pengiriman undangan pasangan tanpa mengorbankan stabilitas grup utama.
    *   Notifikasi kartu penawaran masuk di baris teratas secara langsung: **Terima 💕** atau **Tolak ❌**.
    *   Pembatasan keamanan: Pengguna hanya boleh memiliki maksimal 1 pasangan aktif di dalam kamar yang sama dalam satu waktu.
*   **Logika Cerdas Akumulasi Poin (AlarmViewModel)**:
    *   *Tepat Waktu*: Menghitung deviasi waktu bangun. Memperoleh +10 poin jika bangun sebelum alarm aktif, dan +5 poin jika bangun maksimal 5 menit setelah alarm berbunyi.
    *   *Streak*: Menguji konsistensi pengguna bangun pagi setiap hari. Streak bertambah jika terus berlanjut dan otomatis disetel ulang (reset) ke nol bila terlewat sehari saja.
*   **Widget Home Screen Interaktif (CoupleWidgetProvider)**:
    *   Desain responsif modis dengan sudut membulat, memisahkan ruang pandang pengguna (biru) dan pasangan (merah jambu).
    *   Tombol aksi langsung **"SAYA SUDAH BANGUN ☀️"** yang berjalan mulus melalui `BroadcastReceiver` dan preferensi lokal, memungkinkan sinkronisasi instan ke Firebase tanpa paksaan membuka aplikasi terlebih dahulu.

---

### 🚀 **Versi v1.1.0 (Rilis Mayor Stabil - Berbagi Berkas & Keamanan)**
*Fokus Utama: Arsitektur Transfer Berkas Asimetris Cloud-to-Local dan Kepatuhan Android 14 SDK.*

*   **Sistem Transfer Berkas Asimetris**:
    *   Membagikan klip audio kustom, nada dering mp3, dokumen, atau tugas gambar antar perangkat secara instan tanpa perlu registrasi akun atau masuk (login).
    *   Autogenerasi sandi acak berupa **Kode PIN 6-Digit** sekali pakai.
    *   Limitasi berkas unggahan maksimal sebesar **35 MB** guna mereduksi pemborosan paket data internet dan baterai pengguna.
*   **Dasbor Riwayat Berkas (My Files History)**:
    *   Menghadirkan tab khusus **📁 Berbagi** yang mendaftar secara visual seluruh dokumen terkirim dan terunduh.
    *   Integrasi aksi menu: Bagikan keluar dengan *Android Share Sheets*, kirim ulang berkas luring sekali klik, dan hapus berkas permanen.
*   **Keamanan FileProvider SDK**:
    *   Melindungi aplikasi dari potensi crash `SecurityException` di Android 14 sistem akibat eksposur URI file di luar sandboxed penyimpanan internal.
*   **Survei Dukungan Berlangganan**:
    *   Jendela popup apresiasi otomatis yang mengarahkan masukan umpan balik donasi sukarela pemeliharaan server ke [Takesurvey.html](https://faizinuha.github.io/AlarmGrubing/Takesurvey.html).

---

### 🌸 **Versi v1.0.20 s.d v1.0.28 (Siklus Peluncuran Sakura & Widget Desktop)**
*Fokus Utama: Estetika Visual, Widget Catatan, dan DNS Bypass Server Konten.*

*   **Redesain UI Kawaii & Mascot Cat (v1.0.20)**:
    *   Mengadopsi skema warna pastel pink ceri sakura yang dipadukan dengan aksen gelap onyx.
    *   Penambahan ilustrasi maskot kucing lucu peneman tidur yang tertidur lelap di beranda utama aplikasi.
*   **Widget Desktop Catatan Sakura (v1.0.25 - v1.0.28)**:
    *   Pengenalan widget layar utama fleksibel (ukuran hemat 2x2 dan besar 4x2) untuk memantau catatan harian penting langsung di halaman depan handphone.
*   **DNS Private Overrides (v1.0.21 - v1.0.23)**:
    *   Panduan konfigurasi bypass pembatasan koneksi ISP lokal Indonesia untuk server Firebase, memberikan opsi tautan bebas blokir (Google DNS / Cloudflare DNS) tanpa perlu memasang aplikasi VPN tambahan.
*   **Daya Tahan Hidup di Latar Belakang (v1.0.24)**:
    *   Meminimalkan dampak Doze Mode sistem penghemat baterai Android sehingga penjadwal alarm tetap menyalak tepat waktu di pagi hari.

---

### 👥 **Versi v1.0.7 s.d v1.0.8 (Alpha & Integrasi Luring Berbasis Room)**
*Fokus Utama: Hubungan Kamar Cloud-to-Offline.*

*   **Penyimpanan Luring Room SQLite (v1.0.8)**:
    *   Implementasi kerangka kerja database Room local-first, melancarkan proses pembuatan catatan dan penyiapan alarm meskipun perangkat tidak terhubung ke jaringan internet.
*   **Sinkronisasi Kamar Real-time (v1.0.7)**:
    *   Layanan pembuatan kode kamar unik acak untuk mengelompokkan beberapa perangkat ke dalam satu grup kontrol alarm yang selaras.

---

### ⏰ **Versi v1.0.3 s.d v1.0.6 (Prototipe & Inisiasi Dasar Sistem)**
*Fokus Utama: Arsitektur MVVM dan Manajemen Alarm Native Android.*

*   **Pemisahan Tab UI (v1.0.6)**:
    *   Pengenalan Alur Navigasi Tiga Tab Utama: Alarm Pribadi, Alarm Grup (Kamar), dan Catatan Memo.
*   **Peningkatan Audio Alarm (v1.0.5)**:
    *   Setelan loop volume bunyi bertahap demi kenikmatan tidur dan pemutar nada dering kustom lokal.
*   **Native Alarm Scheduler (v1.0.4)**:
    *   Implementasi sistem alarm latar belakang yang andal memanfaatkan kerangka kerja bawaan `AlarmManager` Android.
*   **Pondasi Dasar (v1.0.3)**:
    *   Membangun struktur bersih Clean Architecture berbasis pola Model-View-ViewModel (MVVM) yang kokoh dan mudah dirawat.

---
*Catatan Perubahan ini diverifikasi secara berkala oleh Tim Pengembang Utama Alarm Grup.*
