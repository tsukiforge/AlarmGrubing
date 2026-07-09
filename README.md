# ⏰ AlarmGrubing (Alarm Grup)

[![Android Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)](https://firebase.google.com)

> **Tidur nyenyak, bangun serempak!** Solusi hibrida alarm pribadi dan kelompok cerdas berbasis *offline-first* dengan sinkronisasi cloud real-time. Katakan selamat tinggal pada telat berjamaah!

---

## 🧐 Kenapa Harus Pakai AlarmGrubing?

Pernahkah kamu merencanakan bangun pagi untuk olahraga, belajar subuh, rapat penting, atau bersiap mengejar kereta, tapi **semua anggota tim malah lanjut tidur karena alarm masing-masing dimatikan**? 

Membangun kebiasaan disiplin itu sulit jika dilakukan sendirian. **AlarmGrubing** hadir sebagai solusinya! Dengan menggabungkan alarm pribadi yang andal, kamar alarm kelompok (Group Alarm), dan fitur penyambung pasangan (Couple Sync), kamu dan tim/teman-temanmu bisa saling mendukung untuk bangun tepat waktu. Begitu satu alarm berdering di kamar grup, semua orang dalam kamar tersebut akan ikut tersinkronisasi. Ditambah sistem perlindungan produktivitas terintegrasi, kamu tidak bisa lagi asal mematikan alarm atau langsung membuka media sosial begitu terbangun!

---

## ✨ Fitur Lengkap & Unggulan Aplikasi

Aplikasi ini dirancang dengan penuh perhatian terhadap detail visual, keandalan sistem, keamanan data pribadi, dan kenyamanan pengguna. Berikut adalah daftar lengkap fitur luar biasa yang ada di dalam AlarmGrubing:

### 👥 1. Kamar Alarm Kelompok (Group Alarm Rooms)
* **Sinkronisasi Kode Unik**: Buat kamar alarm baru atau gabung kamar yang sudah ada cukup dengan membagikan 6 digit kode kamar unik.
* **Bangun Bersama**: Sinkronisasi alarm grup secara instan via cloud sehingga seluruh anggota kamar mendapatkan jadwal alarm yang sama persis.
* **Kolaborasi Tanpa Batas**: Sangat cocok untuk teman kosan, keluarga, kelompok belajar, kelas pagi, hingga tim kerja jarak jauh.
* **Dashboard Status Anggota**: Pantau status "Bangun" vs "Tidur" sesama anggota kamar secara langsung dan real-time.

### 💕 2. Pasangan Tersinkronisasi (Couple Sync Mode)
* **Penyandingan Eksklusif**: Hubungkan alarm berdua dengan kekasih, sahabat, atau pasangan dekatmu di dalam grup secara privat.
* **Skor & Streak Bangun**: Lacak skor kepatuhan bangun bersama dan pertahankan *streak* (hari berurutan) bangun pagi bareng pasanganmu.
* **Sync Bonus Today**: Dapatkan bonus poin harian jika kedua pasangan berhasil bangun tepat waktu!
* **Home Screen Widget**: Widget layar utama HP khusus (*Couple Widget*) yang memperlihatkan status real-time pasangan langsung dari layar utama ponsel Anda.
* **Simulasi Interaktif**: Dilengkapi dengan mode simulasi edukatif yang mempermudah pemahaman cara kerja fitur Couple Sync ini.

### 🔒 3. Modul Kesehatan Sosial & Pembatasan Aplikasi (Health & Social Lock)
* **Jadwal Fokus Produktif**: Jaga konsentrasi dengan membuat jadwal khusus seperti **🎯 Jadwal Fokus**, **📚 Jadwal Belajar**, atau **🎮 Jadwal Bermain**.
* **Pembatas Akses (App Locker)**: Pilih aplikasi tertentu (seperti media sosial atau game) untuk dikunci selama jam fokus berlangsung agar kamu tidak terdistraksi.
* **Kunci Pengaman PIN**: Amankan konfigurasi jadwal produktif ini dengan PIN khusus (4-digit) yang disimpan terenkripsi di berkas lokal JSON agar tidak bisa dimatikan tanpa izin PIN!

### 📝 4. Memo & Catatan Interaktif (Interactive Notes)
* **Memo Sebelum Tidur**: Tulis catatan, ide kreatif, atau tugas penting sebelum tidur sebagai pengingat utama saat kamu terbangun.
* **Desain Kartu Estetik**: Hias kartu catatan dengan berbagai pilihan palet warna kustom yang cantik dan minimalis.
* **Penyimpanan Lokal Room**: Catatan tersimpan dengan aman secara offline-first menggunakan database lokal Room.

### 📁 5. Berbagi Berkas & Ruang Obrolan Kelompok (File Sharing & Group Chat)
* **Kirim Berkas**: Kirim dokumen belajar, berkas PDF, berkas musik, atau gambar ke sesama anggota grup secara aman.
* **Group Chat Instan**: Obrolan santai atau koordinasi strategi bangun pagi langsung di dalam ruang obrolan grup terintegrasi.

### 📱 6. Kustomisasi Wallpaper & Layar Kunci (AOD & Live Video Wallpaper)
* **Live Video Wallpaper**: Pasang video MP4 kesukaan dari galeri pribadi sebagai wallpaper bergerak yang interaktif dan hidup di background ponsel Anda.
* **Kustom Galeri**: Atur foto favoritmu langsung dari galeri HP menjadi wallpaper layar utama.
* **Template Aesthetic AOD bawaan**: Dilengkapi 3 pilihan template layar kunci Always-On Display yang menawan dan estetik.
* **Auto-Restore Wallpaper**: Secara otomatis mengamankan dan mencadangkan wallpaper bawaan sistem Anda, sehingga Anda dapat mengembalikan tampilan asli HP kapan saja dengan sekali ketuk.

### 🎵 7. Musik Kustom dari Penyimpanan HP (Custom Ringtone Selector)
* **Kompak & Ringkas**: Menghilangkan daftar pilihan nada dering bawaan yang berat dengan *Compact Dropdown Menu* yang elegan.
* **Impor File MP3**: Pilih dan gunakan file musik atau lagu kesayangan langsung dari penyimpanan ponselmu agar bangun tidur terasa lebih menyenangkan.

### 🚨 8. Layar Dering Immersive Prioritas Tinggi (Full-Screen Alarm Overlay)
* **Interupsi Layar Penuh**: Menggunakan izin prioritas tinggi `USE_FULL_SCREEN_INTENT` sesungguhnya yang menutupi seluruh status bar dan navigation bar agar kamu segera tersadar.
* **Tahan 2 Detik (Long-Press to Dismiss)**: Anti-snooze tidak sengaja! Kamu harus **menekan dan menahan lingkaran tengah selama 2 detik** untuk mematikan alarm.
* **Snooze Gesture**: Gunakan gerakan geser ke atas (*Swipe Up to Snooze*) yang modern untuk menunda alarm sejenak.

### ⚙️ 9. Navigasi Pintar & Pengaturan Tingkat Lanjut
* **Hardware BackHandler**: Pengendalian tombol fisik "Kembali" ponsel yang cerdas untuk mengurungkan input dialog/form atau kembali ke tab beranda dengan lancar tanpa menutup paksa aplikasi.
* **Foreground Service & Notifikasi Persisten**: Menjamin alarm tetap berdering walau sistem Android memasuki mode tidur lelap (*Doze Mode*) atau RAM dibersihkan.
* **Multibahasa Instan (Indonesian / English)**: Peralihan bahasa antarmuka secara instan tanpa perlu memulai ulang aplikasi secara manual.
* **Automatic Update Checker**: Cek ketersediaan pembaruan aplikasi langsung dari repositori GitHub secara otomatis.
* **Custom Profile Picture**: Ganti foto profil pribadi secara lokal yang aman dari server pihak ketiga (*privacy-first*).

---

## 🛠️ Tumpukan Teknologi (Tech Stack)

* **Bahasa Pemrograman**: [Kotlin](https://kotlinlang.org/) (100% aman, modern, dan tangguh)
* **Arsitektur**: Model-View-ViewModel (MVVM) dengan prinsip *clean code*
* **Framework UI**: [Jetpack Compose](https://developer.android.com/compose) dengan komponen Material Design 3 (M3)
* **Database Lokal**: [Room Database](https://developer.android.com/training/data-storage/room) untuk caching alarm offline-first
* **Sinkronisasi**: [Firebase Realtime Database](https://firebase.google.com/) untuk pengiriman data instan yang andal
* **Layanan Latar Belakang**: `AlarmManager` presisi tinggi dengan `Foreground Service` Android
* **Penyimpanan Lokal**: Berkas JSON terenkripsi lokal untuk pengaturan PIN rahasia dan konfigurasi schedules

---

## ⚙️ Langkah-Langkah Penggunaan & Kompilasi

### Prasyarat
1. Pastikan Anda memiliki **Android Studio Jellyfish** (atau versi yang lebih baru).
2. **JDK versi 17** terpasang di sistem Anda.
3. Perangkat fisik Android atau Emulator dengan **minimum SDK 26 (Android 8.0 Oreo)**.

### Instalasi Cepat
1. **Klon Repositori**
   ```bash
   git clone https://github.com/faizinu61/alarm-grup.git
   cd alarm-grup
   ```
2. **Kompilasi Menggunakan Gradle**
   Jalankan perintah berikut di Terminal Anda:
   ```bash
   gradle assembleDebug
   ```
3. **Pasang APK**
   Temukan hasil kompilasi berkas `.apk` di folder `app/build/outputs/apk/debug/app-debug.apk` lalu pasang di perangkat Anda. Berikan izin akses **Notifikasi** dan **Tampilan di Atas Aplikasi Lain** saat pertama kali dijalankan agar fitur dering layar penuh berjalan sempurna!

---

## 👥 Kontributor & Apresiasi

### Kontributor Utama
| Ilustrasi | Pengembang | Peran & Kontribusi Utama | Kontak |
| :---: | :--- | :--- | :--- |
| 🧑‍💻 | **M. Maichi** | **Lead Architect & Core Developer**<br>• Merancang seluruh struktur alarm, tata letak M3, dan skema Room.<br>• Mengembangkan logika sinkronisasi hibrida offline-first. | [faizinu61@gmail.com](mailto:faizinu61@gmail.com) |
| 🤖 | **Google AI Studio Coding Agent (Antigravity)** | **AI Co-Developer & Integrator**<br>• Mengoptimalkan penanganan gesture dering layar penuh.<br>• Mengintegrasikan deteksi update otomatis dan penanganan jaringan aman. | [AI Studio Build](https://ai.studio/build) |

### Apresiasi Karya Seni (Credits)
* **Doodle Kucing Imut**: Terinspirasi dari karya pembuat asli di [Pinterest Pin 912682680754112540](https://id.pinterest.com/pin/912682680754112540/), direpresentasikan kembali secara interaktif di aplikasi.
* **Aesthetic AOD Wallpapers**: Terinspirasi dari karya kreatif yang dibagikan oleh [The_Honored1 di Pinterest](https://www.pinterest.com/The_Honored1/), digunakan sebagai latar belakang visual estetik.

---

## 📄 Lisensi

Proyek ini dilisensikan di bawah **Lisensi MIT**. Silakan merujuk ke berkas `LICENSE` untuk informasi selengkapnya.

Hak Cipta © 2026 M. Maichi & Kontributor.
