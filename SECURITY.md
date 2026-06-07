# 🛡️ Kebijakan Keamanan (Security Policy) - Alarm Grup

Kami sangat menghargai kontribusi dan partisipasi para peneliti keamanan serta pengguna dalam membantu menjaga keamanan data dan keandalan sistem aplikasi **Alarm Grup**. Kebijakan ini menjelaskan bagaimana kami mengelola laporan kerentanan keamanan dan cara terbaik untuk melaporkannya kepada kami.

---

## 📞 Cara Melaporkan Kerentanan Keamanan

Jika Anda menemukan celah atau kerentanan keamanan pada aplikasi **Alarm Grup**, **JANGAN MEMBUAT ISSUE PUBLIK** di GitHub. Membuat isu publik dapat mengekspos celah tersebut kepada pihak yang tidak bertanggung jawab sebelum perbaikan dirilis.

Silakan kirimkan detail temuan Anda secara privat melalui email salah satu kontributor utama:

- **Kontak Pelaporan**: **faizinu61@gmail.com**
- **Subjek**: `KERENTANAN KEAMANAN: [Nama Celah / Bagian yang Terpengaruh]`

Dalam laporan Anda, mohon sertakan informasi berikut demi mempercepat proses verifikasi:
1. Deskripsi lengkap mengenai potensi kerentanan keamanan yang ditemukan.
2. Langkah-langkah detail untuk mereproduksi masalah tersebut (*Proof of Concept* - PoC).
3. Potensi dampak bagi pengguna (misal: pencurian data kamar grup, manipulasi alarm kelompok lain, atau gangguan layanan).

---

## ⏱️ Siklus Tanggapan Kami

Kami berkomitmen memberikan respons yang cepat dan transparan demi kenyamanan bersama:
- **Konfirmasi Penerimaan**: Kami akan merespons laporan Anda dalam waktu **1 x 24 jam** setelah email diterima untuk mengonfirmasi bahwa kami sedang menyelidiki laporan tersebut.
- **Waktu Penyelidikan & Perbaikan**: Kami berupaya merilis patch keamanan atau solusi sementara dalam waktu **3 hingga 5 hari kerja**, tergantung tingkat keparahan (*severity level*) dari temuan tersebut.
- **Kredit Penghargaan**: Kami dengan bangga akan menyertakan nama Anda sebagai kontributor keamanan khusus di daftar kontributor utama jika laporan yang dikirimkan terbukti valid dan membantu kami meningkatkan pertahanan sistem.

---

## 🔒 Kebijakan Perlindungan Informasi Utama

1. **Privasi Kamar Kelompok**: Kode kamar yang dibagikan antarpengguna dibuat secara unik menggunakan pola acak yang tidak mudah ditebak untuk menghindari keanggotaan kamar yang tidak sah secara kasar (*brute-force*).
2. **Kredensial dan API Key**: Segala kunci penting atau konfigurasi bucket tidak pernah disebarluaskan di tempat terbuka, melainkan dikelola secara khusus menggunakan perlindungan SDK Android dan disalurkan secara tereduksi melalui server penghubung.
3. **Penyimpanan Lokal yang Aman**: Database lokal SQLite dikompresi dan dikelola melalui skema Room internal yang terisolasi dari akses aplikasi lain di luar lingkungan sandbox Android Anda.

---

Terima kasih telah berkomitmen menjaga ekosistem **Alarm Grup** tetap aman, andal, dan menyenangkan bagi semua orang!
