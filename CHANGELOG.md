# 📜 Catatan Perubahan (Changelog) - Alarm Grup

Semua riwayat perubahan, rilis, perbaikan bug, dan optimasi arsitektur aplikasi **Alarm Grup** dari versi dasar hingga versi stabil terbaru.

---

## [5.2.0] - 2026-07-02

### 🚀 Fitur Baru & Peningkatan
* **Wallpaper AOD Animasi Bergerak**: Menambahkan dukungan penuh untuk wallpaper AOD video animasi. File .mp4 dari folder `drawable/Aod/` dipindahkan ke `res/raw/` dan diintegrasikan dengan VideoView sebagai latar belakang AOD yang bergerak (muted, looping, hemat daya).
* **Penyederhanaan Template AOD**: Mengurangi template AOD dari 9 menjadi 3 template bawaan statis (`aod_template_1/2/3.jpg`). Template ekstra (`img_aod_miku_*`, `img_aod_yandere_*`, `img_aod_purple_eye_*`) dihapus.
* **Seksi Wallpaper Terpisah**: Menu pengaturan AOD kini memiliki dua seksi berbeda — "Wallpaper Statis (Bawaan)" untuk template .jpg dan "Wallpaper Animasi (AOD Bergerak)" untuk video animasi.

### 🧹 Pembersihan & Optimasi Aset
* **Hapus 3 File Drawable Tidak Terpakai**: `ic_cat_sleeping.xml`, `ic_cat_yawn.xml`, `img_cute_cat_doodle_*.jpg` — tidak ada referensi di kode.
* **Bersihkan Folder Aod/**: Memindahkan 4 file .mp4 random dari `drawable/Aod/` ke `res/raw/` dengan nama bersih (`aod_anim_1-4.mp4`) dan menghapus folder tersebut.
* **Hapus Template Ekstra**: Hapus 4 file template AOD yang tidak diperlukan (`img_aod_miku_blue`, `img_aod_miku_ripped`, `img_aod_yandere_cleaver`, `img_aod_purple_eye`).

### 📱 Persiapan Store & Metadata
* **Store Metadata**: Menambahkan file `STORE_METADATA.md` berisi deskripsi lengkap untuk Google Play Store & APKPure (nama, short desc, full desc, keywords, kategori, screenshot plan).
* **Update metadata.json**: Deskripsi aplikasi diperbarui dengan listing Play Store & APKPure lengkap.
* **Screenshot Test Otomatis**: Menambahkan `StoreScreenshotTest.kt` menggunakan Roborazzi untuk menghasilkan screenshot UI otomatis (AboutScreen, AodScreen 3 template, AodSettingsScreen) dalam resolusi 1080×1920.
* **Perbaiki App Name**: `strings.xml` diupdate dari "Alarm Sync" menjadi "Alarm Sync — Alarm Grup".

### 🔧 Perbaikan & Peningkatan
* **Kembalikan File Terhapus**: File `img_cat_sleeping.jpg`, `img_cat_yawn.jpg`, `img_cat_happy.jpg`, `img_app_icon_fg_new.jpg`, `img_cute_cat_doodle.jpg` yang masih digunakan di `MainActivity.kt` dan ikon aplikasi telah dikembalikan.

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
