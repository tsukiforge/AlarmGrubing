# 🚨 Alarm Grup (Group Alarm)

Aplikasi manajemen alarm pribadi dan alarm kelompok cerdas yang tersinkronisasi secara real-time melalui teknologi Cloud, didukung oleh sistem failover offline mandiri yang andal dan aman.

## 🌟 Fitur Utama

- **⏰ Alarm Pribadi (Personal Alarm)**: Penjadwalan alarm mandiri di perangkat lokal demi menjaga kepatuhan jadwal pribadi Anda secara independen.
- **👥 Kamar Alarm Kelompok (Group Alarm Rooms)**: Buat atau gabung ke kamar kelompok menggunakan kode unik untuk menyinkronkan alarm bangun tidur, kelas, atau rapat bersama teman, kolega, atau keluarga Anda secara instan.
- **🔄 Sinkronisasi Real-Time & Hibrida**: Menggunakan backend REST API yang tangguh untuk sinkronisasi alarm kelompok. Dilengkapi visual status indikator sinkronisasi (Tersinkron, Sinkronisasi, Koneksi Terputus).
- **📶 Detektor Koneksi Akurat (Smart Off-line/On-line Handshake)**:
  - Otomatis mendeteksi media koneksi aktif seperti **Wi-Fi 📶** atau **Data Seluler 📱**.
  - Menyediakan pemberitahuan status **Offline ⚠️** jika koneksi mati saat aplikasi berjalan maupun ketika keluar.
  - Memungkinkan pembuatan Kamar Grup secara Offline (disimpan lokal sementara) dan menyediakan tombol sekali sentuh **"Online-kan Grup & Sinkron Server 🔄"** untuk dipublikasikan ke awan setelah jaringan Anda kembali aktif.
- **🔔 Tampilan Dering Prioritas Tinggi (Full-Screen Ringing Overlay)**:
  - Mengimplementasikan `USE_FULL_SCREEN_INTENT` sehingga alarm berdering akan langsung memotong layar kunci (*lock screen*) atau muncul secara kokoh di lapisan teratas (*top bar/foreground*).
  - Dilengkapi antarmuka pemutus alarm cepat berukuran ergonomis guna memastikan kemudahan penggunaan saat pertama kali terbangun.
- **⚡ Keandalan Sistem Latar Belakang**:
  - Memanfaatkan `AlarmManager` presisi standar Android (`SCHEDULE_EXACT_ALARM` & `USE_EXACT_ALARM`).
  - Diterapkan menggunakan `Foreground Service` tangguh lengkap dengan notifikasi persisten agar proses dering tidak dihentikan oleh pembatas daya otomatis Android (*Doze Mode*).
- **🔄 Pengecekan Update & Kesalahan Jaringan Terdistribusi**: Dilengkapi modul pengecekan pembaruan rilis terotomatisasi secara simultan serta penanganan kesalahan jaringan yang proaktif di semua fitur inti.

---

## 🛠️ Arsitektur & Teknologi

Aplikasi ini dibangun menggunakan tumpukan teknologi modern berstandar industri:

- **Bahasa Utama**: [Kotlin](https://kotlinlang.org/) (100% Type-safe & Modern).
- **Desain UI**: [Jetpack Compose](https://developer.android.com/compose) dengan sistem dekorasi warna dinamis berbasis **Material Design 3 (M3)** yang elegan dan minimalis.
- **Penyimpanan Lokal (Database)**: [Room Database](https://developer.android.com/training/data-storage/room) yang mendukung transaksi aman dan penanganan coroutine Kotlin secara terintegrasi.
- **Koneksi Jaringan**: [Retrofit 2](https://square.github.io/retrofit/) & [OkHttp3](https://square.github.io/okhttp/) dengan penanganan intersepsi logging berkecepatan tinggi.
- **Asinkron & Flow**: Kotlin Coroutines & Flow (`StateFlow`/`SharedFlow`) untuk mendengarkan perubahan status jaringan dan sinkronisasi berkala secara non-blocking.

---

## 👥 Kontributor Pembawa Perubahan (Real Contributors)

Proyek ini terwujud berkat dedikasi kolaboratif pengembang berbakat berikut:

| Foto / Ilustrasi | Kontributor | Peran & Kontribusi Utama | Kontak |
| :---: | :--- | :--- | :--- |
| 🧑‍💻 | **M. Faizin (Faiz)** | **Lead Architect & Lead Core Developer**<br>• Pencetus ide utama sistem alarm sinkronisasi kelompok.<br>• Merancang tata letak visual antarmuka Material 3, skema database Room lokal, dan integrasi penanganan runtime alarm.<br>• Mengembangkan logika hibrida failover offline untuk kamar grup. | [faizinu61@gmail.com](mailto:faizinu61@gmail.com) |
| 🤖 | **Google AI Studio Coding Agent (Antigravity)** | **AI Co-Developer & Integrator**<br>• Membantu otomatisasi perancangan struktur API sinkronisasi REST.<br>• Mengoptimalkan penanganan izin penuh layar (`Full-Screen Intent`) Android.<br>• Memastikan kepatuhan kode dan kelancaran proses kompilasi Gradle secara berkala. | [Google AI Studio Build](https://ai.studio/build) |

---

## 🛠️ Langkah-langkah Penginstalan & Kompilasi

1. **Prasyarat**:
   - Pastikan Anda menggunakan Android Studio Jellyfish (atau versi lebih baru).
   - JDK versi 17 atau yang kompatibel.
   - Perangkat pengujian Android dengan SDK minimum 26 (Android 8.0 Oreo).
2. **Klon Repositori**:
   ```bash
   git clone https://github.com/faizinu61/alarm-grup.git
   cd alarm-grup
   ```
3. **Kompilasi Proyek**:
   Jalankan perintah Gradle lewat terminal:
   ```bash
   gradle assembleDebug
   ```
4. **Jalankan Aplikasi**:
   Instal file APK yang dibuat ke perangkat Anda, berikan izin akses Notifikasi dan Tampilan di Atas Aplikasi Lain jika diminta, lalu rasakan pengalaman baru bangun tidur tepat waktu bersama kelompok Anda!

---

## 📄 Lisensi

Hak Cipta © 2026 M. Maichi & Kontributor. Distribusikan di bawah Lisensi MIT. Baca file `LICENSE` untuk informasi lebih lanjut.
