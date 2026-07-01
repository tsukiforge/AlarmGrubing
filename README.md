# 🚨 Alarm Grup (Group Alarm)

Aplikasi manajemen alarm pribadi dan alarm kelompok cerdas yang tersinkronisasi secara real-time melalui teknologi Cloud, didukung oleh sistem failover offline mandiri yang andal dan aman.

## 🌟 Fitur Utama

- **⏰ Alarm Pribadi (Personal Alarm)**: Penjadwalan alarm mandiri di perangkat lokal demi menjaga kepatuhan jadwal pribadi Anda secara independen.
- **👥 Kamar Alarm Kelompok (Group Alarm Rooms)**: Buat atau gabung ke kamar kelompok menggunakan kode unik untuk menyinkronkan alarm bangun tidur, kelas, atau rapat bersama teman, kolega, atau keluarga Anda secara instan.
- **🔄 Sinkronisasi Real-Time & Hibrida**: Menggunakan backend Firebase Realtime Database andal yang dijamin cepat dan bebas blokir oleh seluruh ISP di Indonesia, dilengkapi visual status indikator sinkronisasi.
- **⚙️ Halaman Pengaturan Terpisah (Dedicated Settings Screen)**:
  - Menu konfigurasi yang dipisahkan ke halaman tersendiri dengan animasi transisi *fade-in* dan *fade-out* bergaya modern guna menjaga aplikasi tetap super ringan dan hemat RAM.
  - Memungkinkan penyesuaian Display Name, mode tema (Terang, Gelap, Sistem), animasi sakura, server sinkronisasi kustom, serta izin berdering di background.
- **👤 Foto Profil Lokal & Mandiri (Secure Offline-First Avatars)**:
  - Pengguna dapat mengunggah foto profil kustom sendiri.
  - Demi menjaga keamanan, integritas data pribadi, dan privasi penuh, setiap foto yang diunggah disimpan di folder lokal aman terisolasi perangkat (`context.filesDir`), memastikan foto Anda sepenuhnya aman dan rapi meskipun server online mati.
- **📁 Musik Kustom dari HP & Dropdown Ringkas (Custom Notification Sounds)**:
  - Mengganti daftar pilihan nada dering panjang yang berat dengan *Compact Dropdown Menu* yang minimalis.
  - Memungkinkan pengguna mengimpor lagu kesayangan/musik MP3 secara langsung dari penyimpanan HP lokal untuk dijadikan nada dering alarm kustom.
- **🔔 Tampilan Dering Prioritas Tinggi & Real Full Screen**:
  - Mengimplementasikan `USE_FULL_SCREEN_INTENT` yang sesungguhnya dengan tampilan layar penuh (*Immersive Full-Screen*), memotong lapisan status/navigation bar secara kokoh untuk memastikan Anda segera bangun.
  - Dilengkapi antarmuka pemutus alarm cepat berukuran ergonomis guna memastikan kemudahan penggunaan saat pertama kali terbangun.
- **⚡ Keandalan Sistem Latar Belakang**:
  - Memanfaatkan `AlarmManager` presisi standar Android (`SCHEDULE_EXACT_ALARM` & `USE_EXACT_ALARM`).
  - Diterapkan menggunakan `Foreground Service` tangguh lengkap dengan notifikasi persisten agar proses dering tidak dihentikan oleh pembatas daya otomatis Android (*Doze Mode*).
- **🔄 Pengecekan Update & Kesalahan Jaringan Terdistribusi**: Dilengkapi modul pengecekan pembaruan rilis terotomatisasi secara simultan serta penanganan kesalahan jaringan yang proaktif di semua fitur inti. Aliran pengecekan kini dibekali fitur penanganan kesalahan aman dari HTTP 404, menghilangkan notifikasi kegagalan jika repositori rilis belum terbit di GitHub.

---

## 🚀 Pembaruan Sistem, Keamanan & CI/CD Terkini

Kami terus melakukan optimalisasi untuk menghadirkan kualitas aplikasi terbaik, andal, dan aman:

### 1. 🔧 Penanganan Kesalahan & Keamanan Pengecekan Update (Fix 404 Error)
* **Status Masalah**: Sebelumnya, modul `GithubUpdateChecker` memicu peringatan error HTTP 404 jika repositori GitHub belum memiliki rilis resmi (GitHub Releases).
* **Solusi**: Diimplementasikan logika penanganan aman (*graceful fallback*): jika respons GitHub mengembalikan 404, aplikasi secara cerdas mengonversi status tersebut sebagai penanda bahwa aplikasi telah berada pada versi terbaru (`hasUpdate = false`), bukan kegagalan sistem. Pengalaman pengguna kini lebih bersih dan bebas dari dialog galat tak terduga.

### 2. 🎨 Penyempurnaan Tema Profil (Clean Smooth Theme)
* Menyederhanakan opsi tema grafis pada halaman profil pengguna dengan menghapus pilihan warna tema yang kurang konsisten (Starlight dan System).
* Menyisakan dua mode visual terpoles sempurna:
  * **Putih Smooth (☀️)**: Tampilan terang yang lembut di mata dengan kontras seimbang.
  * **Gelap (🌙)**: Tampilan gelap pekat yang hemat daya dan bersahabat untuk aktivitas malam hari.

### 3. 🤖 Pipeline Otomatisasi CI/CD & Rilis (.github/workflows/release.yml)
* **Alur Kerja GitHub Actions**: Pengunggahan rilis otomatis terpicu setiap kali tag versi baru berformat `v*` (contoh: `v1.0.0`) dikirim ke repositori.
* **Kompilasi Otomatis**: Pipeline otomatis membangun Debug APK (`./gradlew assembleDebug`) secara mandiri pada wadah Cloud Ubuntu yang terisolasi dan aman.
* **Generasi Changelog Cerdas**: Sistem secara otomatis mengekstrak catatan komit (commit logs) sejak versi rilisan terakhir dan membaginya ke dalam kelompok:
  * 🚀 **Fitur Baru**: Untuk komit berlabel penambahan fitur (`feat`, `feature`, `tambah`, `add`, `fitur`).
  * 🔧 **Perbaikan (Bug Fixes)**: Untuk komit perbaikan masalah (`fix`, `bug`, `perbaikan`, `solve`, `resolve`).
  * 📝 **Pemberitahuan Lainnya**: Untuk komit pemeliharaan umum lainnya.
* **Manajemen Rilis**: Hasil kompilasi APK dideploy secara instan ke portal rilis GitHub disertai catatan changelog otomatis.

### 📌 Aturan Penamaan Versi (Semantic Versioning - x.y.z):
* **Patch (x.y.z -> eg: 1.0.1)**: Untuk **perbaikan bug (fix)** minor maupun pembenahan stabilitas sistem.
* **Minor (x.y.z -> eg: 1.1.0)**: Untuk **penambahan fitur baru (tambah fitur)** secara backward-compatible.
* **Major (x.y.z -> eg: 2.0.0)**: Untuk perombakan arsitektural besar atau perubahan yang tidak kompatibel ke belakang.

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
| 🧑‍💻 | **M. Maichi** | **Lead Architect & Lead Core Developer**<br>• Pencetus ide utama sistem alarm sinkronisasi kelompok.<br>• Merancang tata letak visual antarmuka Material 3, skema database Room lokal, dan integrasi penanganan runtime alarm.<br>• Mengembangkan logika hibrida failover offline untuk kamar grup. | [faizinu61@gmail.com](mailto:faizinu61@gmail.com) |
| 🤖 | **Google AI Studio Coding Agent (Antigravity)** | **AI Co-Developer & Integrator**<br>• Membantu otomatisasi perancangan struktur API sinkronisasi REST.<br>• Mengoptimalkan penanganan izin penuh layar (`Full-Screen Intent`) Android.<br>• Memastikan kepatuhan kode dan kelancaran proses kompilasi Gradle secara berkala. | [Google AI Studio Build](https://ai.studio/build) |

---

## 🎨 Apresiasi Karya Seni (Art Credits)

Ilustrasi maskot kucing imut dan wallpaper AOD di dalam aplikasi ini terinspirasi dari karya seni berikut:
- **Doodle Kucing Imut**: Terinspirasi dari/merujuk pada karya pembuat asli di [Pinterest Pin 912682680754112540](https://id.pinterest.com/pin/912682680754112540/), direpresentasikan kembali demi visualisasi estetik di dalam aplikasi.
- **AOD Aesthetic Wallpapers**: Terinspirasi dari karya kreatif luar biasa yang dibagikan oleh [The_Honored1 di Pinterest](https://www.pinterest.com/The_Honored1/), direpresentasikan secara digital sebagai template background Always-On Display (AOD) estetik.

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
