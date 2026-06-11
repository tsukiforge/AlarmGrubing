#!/bin/bash

# ==============================================================================
# Script Pembuat Keystore Rilis Otomatis & Encode Base64
# Untuk Aplikasi: Alarm Grup (AlarmGrubing)
# ==============================================================================

# Menentukan variabel default
KEYSTORE_NAME="my-upload-key.jks"
KEY_ALIAS="upload"
VALIDITY_DAYS=10000

echo "======================================================="
echo " Pembuatan Keystore Rilis Baru & Auto Base64 Encoder"
echo "======================================================="
echo ""

# Meminta input password dari pengguna jika ingin diubah
read -p "Masukkan password Keystore (minimal 6 karakter) [default: android]: " STORE_PASSWORD
STORE_PASSWORD=${STORE_PASSWORD:-android}

read -p "Masukkan password Kunci (Key Password) [default: android]: " KEY_PASSWORD
KEY_PASSWORD=${KEY_PASSWORD:-android}

echo ""
echo "Sedang men-generate Keystore: $KEYSTORE_NAME..."
echo "Sertifikat berlaku selama: $VALIDITY_DAYS hari..."

# Melakukan generate keystore menggunakan keytool bawaan JDK/Android Studio
keytool -genkey -v \
  -keystore "$KEYSTORE_NAME" \
  -alias "$KEY_ALIAS" \
  -keyalg RSA \
  -keysize 2048 \
  -validity "$VALIDITY_DAYS" \
  -storepass "$STORE_PASSWORD" \
  -keypass "$KEY_PASSWORD" \
  -dname "CN=Faizin, OU=Development, O=FaizinDev, L=Jakarta, S=DKI, C=ID"

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ BERHASIL: Keystore '$KEYSTORE_NAME' telah berhasil dibuat!"
    echo "-------------------------------------------------------"
    echo "DETAIL INFORMASI KE AKSES GITHUB SECRETS:"
    echo "1. KEY_ALIAS     : $KEY_ALIAS"
    echo "2. STORE_PASSWORD: $STORE_PASSWORD"
    echo "3. KEY_PASSWORD  : $KEY_PASSWORD"
    echo "-------------------------------------------------------"
    echo ""
    
    # Deteksi operating system untuk command base64 yang tepat
    echo "Sedang meng-encode keystore ke format Base64..."
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        BASE64_OUTPUT=$(base64 -i "$KEYSTORE_NAME")
    else
        # Linux / WSL / Git Bash di Windows
        BASE64_OUTPUT=$(base64 -w 0 "$KEYSTORE_NAME" 2>/dev/null || base64 "$KEYSTORE_NAME")
    fi
    
    # Menyimpan output base64 ke file teks untuk kemudahan copy-paste
    echo "$BASE64_OUTPUT" > keystore_base64.txt
    
    echo ""
    echo "✅ BERHASIL: File Base64 disimpan di: keystore_base64.txt"
    echo "Silakan buka file 'keystore_base64.txt' lalu salin (copy) SELURUH isinya"
    echo "dan masukkan sebagai nilai 'RELEASE_KEYSTORE_BASE64' di GitHub Secrets!"
    echo "-------------------------------------------------------"
else
    echo "❌ GAGAL: Terjadi masalah waktu menjalankan 'keytool'."
    echo "Pastikan Anda sudah menginstal JDK (Java Development Kit) dan 'keytool' bisa diakses dari Terminal Anda."
fi
