# 📜 Catatan Perubahan (Changelog) - Alarm Grup

Semua riwayat perubahan, rilis, perbaikan bug, dan optimasi arsitektur aplikasi **Alarm Grup** dari versi dasar hingga versi stabil terbaru.

---

## [5.4.0] - 2026-07-12

### 🚀 Fitur Baru & Peningkatan
* **Health Social Enforcement (Penguncian Aplikasi Berjadwal)**: Sistem penguncian aplikasi produktif penuh berbasis jadwal (Fokus, Belajar, Screen Time) yang diamankan dengan enkripsi PIN. Mencakup foreground service dengan UsageStatsManager untuk monitoring real-time, overlay kunci full-screen via WindowManager (TYPE_APPLICATION_OVERLAY), serta verifikasi PIN melalui overlay activity terpisah.
* **Quick Settings Tile (AppLock)**: Tile Quick Settings Android untuk mengunci aplikasi langsung dari panel notifikasi. Mendeteksi aplikasi foreground via UsageStatsManager dan langsung memicu overlay kunci yang sama.
* **AlarmManager Schedule untuk Health Social**: Penjadwalan start/stop monitoring service menggunakan AlarmManager.setExactAndAllowWhileIdle dengan BOOT_COMPLETED receiver untuk re-register alarm setelah restart perangkat.
* **Material 3 TimePicker (Dial Mode)**: Mengganti custom OutlinedTextField untuk jam jadwal dengan Material 3 TimePicker dial mode bawaan Compose Material3, memberikan pengalaman pemilihan waktu yang lebih intuitif dan modern.
* **Notifikasi Mode Health Social**: Notifikasi otomatis saat jadwal dimulai (mode fokus aktif) dan jadwal berakhir (mode fokus selesai), membantu pengguna tetap sadar akan perubahan status pembatasan aplikasi.
* **Penyimpanan Bahasa Persisten (Locale Persistence)**: Implementasi custom Application class (AlarmGrubingApp) dengan override attachBaseContext() untuk mengaplikasikan bahasa yang dipilih sebelum UI dirender, memastikan bahasa tetap bertahan meskipun aplikasi di-restart atau proses Android dihentikan.
* **AppLocalesMetadataHolderService**: Dukungan autoStoreLocales untuk Android 13+ melalui metadata service, serta locales_config.xml dengan dukungan bahasa Indonesia, Inggris, Spanyol, Perancis, dan Arab.

### 🛠️ Perbaikan Bug
* **Perbaikan UI Snooze**: Area swipe snooze di RingingOverlay tidak lagi terbungkus card visual (background gelap, border, rounded shape). Area swipe kini full-width dan mengisi sisa layar, sehingga pengguna dapat swipe di mana saja tanpa hambatan visual.
* **Pencegahan Double Alarm**: Menambahkan guard di AlarmRingingService.onStartCommand() untuk menolak duplicate trigger alarm saat layar dering sudah aktif, mencegah alarm berbunyi dua kali secara bersamaan.

### ⚡ Optimasi Kinerja & Stabilitas (v5.4.0 Hotfix)
* **Perbaikan Freeze/Lag Akibat Multiple Monitoring Loop**: Menambahkan guard `if (isRunning) return START_STICKY` dan `monitoringJob?.cancel()` sebelum membuat job baru di `HealthSocialMonitorService.onStartCommand()`. Juga menambahkan rekreasi `CoroutineScope` jika sudah di-cancel, mencegah tumpukan loop monitoring berjalan bersamaan yang menyebabkan lag parah.
* **Optimasi Query Foreground App**: Mengganti `queryUsageStats(INTERVAL_DAILY)` yang berat dengan `queryEvents()` yang hanya mengambil event `MOVE_TO_FOREGROUND` — jauh lebih ringan tanpa scan riwayat harian penuh. Interval polling dinaikkan dari 2 detik ke 3 detik.
* **Overlay Dipindah ke Main Thread**: Memindahkan semua pemanggilan `HealthSocialOverlayManager.showLockOverlay()` dan `dismissOverlay()` ke `Dispatchers.Main` dengan `withContext()`, karena operasi `WindowManager.addView`/`removeView` wajib dari thread yang memiliki Looper.
* **Cegah Re-render Overlay Berulang**: Menambahkan tracking `lastLockedApp` dan pengecekan `isOverlayShowing()` untuk menghindari inflate ulang overlay setiap 3 detik jika aplikasi terkunci yang sama masih terbuka. Overlay juga otomatis di-dismiss saat aplikasi foreground berubah ke bukan app terkunci.

### 🌐 Penyederhanaan Bahasa
* **Dukungan Bahasa Dikurangi ke 3 Bahasa**: `locales_config.xml` hanya menyertakan Indonesia (`id`), Inggris (`en`), dan Jepang (`ja`). Bahasa Spanyol, Portugis, Perancis, Jerman, Rusia, Arab, dan Mandarin dihapus beserta file resource string-nya.
* **Default Fallback ke Indonesia**: Jika bahasa perangkat bukan salah satu dari 3 bahasa yang didukung, aplikasi otomatis fallback ke Bahasa Indonesia (bukan Inggris).
* **Pilihan Bahasa di UI**: Opsi bahasa di Settings dikurangi dari 11 menjadi 4 (Ikuti Sistem, Indonesia, English, 日本語).

## [5.3.0] - 2026-07-09

### 🚀 Fitur Baru & Peningkatan
* **Peralihan Bahasa Instan (Instant Locale Switching)**: Mendukung pengubahan bahasa secara langsung (Indonesian <-> English) dengan penyimpanan preferensi kustom terpusat dan pemuatan ulang aktivitas (`Activity.recreate()`) secara dinamis tanpa perlu memulai ulang ponsel secara manual.
* **Sistem Navigasi Cerdas (Hardware Back Handler)**: Menambahkan Jetpack Compose `BackHandler` untuk penanganan tombol fisik "Kembali" pintar. Tombol kembali kini mengurungkan input dialog, menutup pop-up edit alarm, atau menavigasi kembali ke tab Beranda secara otomatis sebelum keluar dari aplikasi.
* **Metode Tahan 2 Detik Layar Dering (Long-Press to Dismiss)**: Meningkatkan mekanisme keamanan mematikan alarm pada layar dering penuh (*Full-Screen Alarm Overlay*). Pengguna kini harus menahan lingkaran tengah selama 2 detik untuk menghindari ketidaksengajaan mematikan alarm saat masih setengah mengantuk.
* **Kebijakan Profil Bersih (Clean Profile Management)**: Menyederhanakan alur pengaturan foto profil dengan menghapus tombol hapus foto yang berlebihan, memastikan integrasi media penyimpanan lokal tetap bersih dan aman (*privacy-first*).

---

## [5.2.60] - 2026-07-06

### 🚀 Fitur Baru & Peningkatan
* **Live Video Wallpaper & Live Wallpaper Manager**: Mendukung penyetelan video MP4 kustom langsung dari galeri sebagai wallpaper dinamis berkinerja tinggi. Dilengkapi dengan fungsionalitas auto-restore cerdas untuk mengembalikan wallpaper asli sistem kapan saja secara instan tanpa mengganggu konfigurasi launcher.
* **Sakura Floating Animation Overlay**: Lapisan kelopak bunga sakura dinamis yang mengambang dengan indah di background aplikasi, memberikan estetika modern, damai, dan rileks selama navigasi antarmuka.
* **Layar Siaga Always-On Display (AOD) Terpadu**: Menyinkronkan gambar visual latar belakang kustom dengan jam anti-burn-in dan kata-kata motivasi harian yang dinamis.

---

## [5.2.20] - 2026-07-03

### 🚀 Fitur Baru & Peningkatan
* **Catatan & Memo Terintegrasi (Room Notes)**: Tab Catatan interaktif untuk menulis memo, ide, atau tugas sebelum tidur menggunakan database Room SQLite dengan penyesuaian warna kartu estetik yang minimalis.
* **Modul Keamanan Health & Social Lock**: Penguncian aplikasi penarik perhatian (seperti media sosial, video, game) berbasis jadwal produktif (Fokus, Belajar, Bermain) yang diamankan oleh enkripsi PIN berkas lokal JSON berkekuatan tinggi.

---

## [5.2.0] - 2026-07-01

### 🚀 Fitur Baru & Peningkatan
* **Penyandingan Couple Sync Unggulan**: Koneksi eksklusif berdua dalam grup, melacak skor kepatuhan, serta perhitungan hari bangun berurutan (streak) untuk memotivasi satu sama lain secara real-time.
* **Home Screen Couple Widget**: Widget berukuran ringkas yang menampilkan sisa baterai pasangan serta status bangun/tidur mereka secara langsung di layar beranda.
* **Group Chat & Secure File Sharing**: Pengiriman pesan instan serta dokumen penting secara instan dengan proteksi lokal antar anggota grup kamar.

---

## [5.1.0] - 2026-06-30

### 🚀 Fitur Baru & Peningkatan
* **Always-On Display (AOD) Otomatis**: Layar standby AOD sekarang aktif secara otomatis saat layar HP dimatikan (dikunci) menggunakan `AodService` berbasis `BroadcastReceiver` (`ACTION_SCREEN_OFF`), menghadirkan pengalaman AOD yang seamless dan native tanpa repot.
* **Desain Anti-Burn-In & Perlindungan Layar**: Jam dan tanggal berganti posisi setiap 60 detik secara cerdas untuk mencegah penumpukan piksel (burn-in) dan menjaga keawetan layar HP.
* **Wallpaper Kustom & Template Estetik**: Menambahkan dukungan penuh untuk wallpaper kustom yang dipilih langsung dari Galeri HP pribadi (*Custom Wallpaper*) serta 5 pilihan template bawaan yang cantik.
* **Widget Kata Motivasi**: Menambahkan kata-kata motivasi harian yang berganti secara berkala di layar standby AOD dengan opsi nyala/mati.
* **Pengaturan AOD yang Ramping**: Antarmuka konfigurasi diperbarui dengan tata letak vertikal scrollable yang praktis, menghapus teks/tombol yang menghalangi, serta digantikan dengan sakelar (Switch) ramping untuk kendali intuitif. Juga ditambahkan pintasan sakelar (Switch) On/Off AOD di halaman Pengaturan & Profil utama untuk kenyamanan ekstra.
* **Quick Share Widget**: Tombol Quick Share dipindahkan dari menu Pengaturan utama dan ditransformasikan menjadi Widget Home Screen interaktif yang murni untuk efisiensi ruang navigasi.
* **Penyempurnaan Navigasi**: Navigasi bawah diperbarui agar tidak terlalu padat dengan penambahan spasi (*padding*) horizontal dan fitur scroll horizontal (*LazyRow* style).
* **Pembaruan Versi**: Penyesuaian skema versi menjadi 5.1.0 untuk sinkronisasi akurat dengan rilis repositori utama.

---

## [1.1.51] - 2026-06-27

### 🚀 Fitur Baru & Peningkatan
* **Widget Layar Utama (Home Screen)**: Menambahkan Widget Cuaca (`WeatherWidgetProvider`) yang menyesuaikan background berdasarkan kondisi aktual, serta Widget Motivasi (`MotivationWidgetProvider`) untuk menampilkan kutipan harian langsung di layar utama perangkat.
* **Pelacakan Baterai Pasangan (Couple Widget)**: Menambahkan fitur sinkronisasi dan pelacakan sisa persentase baterai pasangan (`batteryLevel`) yang ditampilkan secara real-time dan dinamis (indikator 🔋/🪫) langsung di Couple Widget.
* **Integrasi Lokasi Akurat**: Memperbarui izin sistem (`ACCESS_COARSE_LOCATION` & `ACCESS_FINE_LOCATION`) agar sinkronisasi suhu dan cuaca di Widget Cuaca dan aplikasi menjadi lebih presisi secara lokal.
* **Aset Cuaca Estetik**: Menambahkan aset grafis visual cuaca (Cerah, Berawan, Hujan) bergaya estetika anime pagi hari untuk mendukung UI Widget Cuaca.
* **Penyederhanaan Dashboard**: Memindahkan fitur motivasi sepenuhnya ke Widget Home Screen agar tampilan Dashboard utama lebih bersih dan fungsional.

## [1.1.50] - 2026-06-24

### 🚀 Fitur Baru & Peningkatan
* **Fitur Bangunkan Anggota & Limitasi Getar**: Tombol "Bangunkan 🔔" kini memunculkan seluruh daftar anggota kamar grup, dilengkapi sistem pembatasan (*cooldown*) global maksimal mengirim alarm sinyal getar 2 kali sehari per pengguna.
* **Penyederhanaan UI Kamar Grup**: Menyederhanakan UI dialog "Kelola & Bangunkan Kamar" dengan menghapus indikator status merah/hijau (SUDAH BANGUN/BELUM BANGUN) pada anggota yang belum pairing agar tampilan lebih ringkas dan praktis.
* **Keandalan Latar Belakang (Doze Mode Bypass)**: Mengintegrasikan `PowerManager.PARTIAL_WAKE_LOCK` dengan pelepasan otomatis untuk menjamin alarm tetap berbunyi presisi dan tepat waktu saat perangkat dalam masa tidur nyenyak (*Doze Mode*) atau mode penghemat daya ekstrim.
* **Simulasi Couple Sync Interaktif**: Menyediakan panel kontrol simulasi mandiri di tab kamar alarm grup untuk menguji dinamika tidur/bangun karakter "Leon" dan "Mia" serta perhitungan bonus poin (+15 poin).
* **Personalisasi Nada Dering**: Memperbarui teks sambutan layar penuh alarm dengan sapaan hangat: `"bangunn sayanggg Miaw~ ✨"`.
* **Optimasi Ukuran Paket (ABI Splits)**: Mengonfigurasi mekanisme pemisahan otomatis paket instalasi di `build.gradle.kts` agar menghasilkan binary yang dioptimalkan khusus untuk arsitektur CPU perangkat cerdas:
  * `arm64-v8a`: Performa prima untuk ponsel Android modern 64-bit.
  * `armeabi-v7a`: Kompatibilitas optimal untuk ponsel Android 32-bit.
  * `universal`: Paket gabungan all-in-one untuk seluruh arsitektur.

### 🛠️ Perbaikan Bug & Sistem
* **Sistem Update Otomatis**: Memperbaiki logika perbandingan versi pada `GithubUpdateChecker` (`canonicalizeVersion`) menggunakan standar Semantic Versioning agar pembaruan rilis OTA terdeteksi dengan akurat.
* **Responsivitas Landing Page**: Melengkapi landing page (`index.html`) dengan helper skrip kalkulasi versi pemasaran otomatis berdasarkan patch aktif.
* **Kompabilitas Skrip CI/CD (Restore standard APK filenames)**: Menambahkan kustom task Gradle `CopyApkTask` yang ramah *Configuration Cache* untuk secara otomatis menggandakan dan menyediakan file `app-debug.apk` dan `app-release.apk` dari build universal, sehingga skrip Runner/Publishing tidak mengalami error pencarian file.

---

## [1.1.34] - 2026-06-15

### 🚀 Fitur Baru
* **Modul Couple Sync**: Sinkronisasi status alarm multi-pengguna secara real-time.
* **QR Room Sharing**: Membagikan akses grup kamar langsung menggunakan kode QR dinamis.
* **Penyimpanan Lokal Room**: Migrasi penuh penyimpanan lokal ke Room Database SQLite.

---

## [1.1.0] - 2026-05-20

### 🚀 Peningkatan Layanan
* **Cloud Sync Integration**: Sinkronisasi basis data online real-time untuk koordinasi alarm lintas perangkat.
* **UI Refresh**: Penyegaran elemen Material 3 dengan tatanan visual yang lebih kontras dan dinamis.

---

## [1.0.24] - 2026-04-12

### 🛠️ Perbaikan Bug
* **Sistem Ekstraksi ZIP**: Penanganan kompresi multi-berkas sebelum ditransmisikan agar lebih hemat kuota data internet.
* **Izin Runtime Android**: Memperketat penanganan permintaan izin akses media di Android 13+.

---

## [1.0.4] - 2026-03-05

### 🛠️ Perbaikan Bug
* **Koreksi API Handler**: Hotfix penanganan kesalahan tautan rilis tidak ditemukan saat memeriksa versi terbaru dari repositori online.

---

## [1.0.3] - 2026-02-18

### 🚀 Rilis Awal
* Standardisasi antarmuka dasar jam alarm mandiri.
* Fitur pengaturan waktu dasar dan getaran kustom.
