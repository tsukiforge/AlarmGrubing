package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.webkit.MimeTypeMap
import com.example.data.api.NetworkClient
import com.example.data.model.FileTransferData
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.InputStream

class FileTransferRepository(private val context: Context) {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(FileTransferData::class.java)

    private val sharedDir: File = File(context.filesDir, "shared_files").apply {
        if (!exists()) mkdirs()
    }

    /**
     * Upload a local file from a Content Uri or File path to the cloud DB
     */
    suspend fun uploadFile(
        uri: Uri,
        customCode: String,
        senderName: String,
        onProgress: (String) -> Unit
    ): Result<String> {
        return try {
            onProgress("Mempersiapkan berkas...")
            val contentResolver = context.contentResolver
            
            // Get file name and size
            var fileName = "berkas_tanpa_nama"
            var fileSize: Long = 0
            
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex)
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }

            // Restrict file size to 35 MB to prevent OOM
            val maxSizeBytes = 35 * 1024 * 1024 // 35 MB
            if (fileSize > maxSizeBytes) {
                return Result.failure(Exception("Berkas terlalu besar (${String.format("%.2f", fileSize / (1024f * 1024f))} MB). Batas gratis maksimal adalah 35 MB."))
            }

            // Get file MimeType
            val fileType = contentResolver.getType(uri) ?: "application/octet-stream"

            onProgress("Membaca berkas (Base64)...")
            val inputStream = contentResolver.openInputStream(uri) 
                ?: return Result.failure(Exception("Gagal membuka berkas input"))
            
            val bytes = inputStream.readBytes()
            inputStream.close()

            onProgress("Mengonversi data...")
            val base64Data = Base64.encodeToString(bytes, Base64.DEFAULT)

            onProgress("Mengunggah secara aman...")
            val transferData = FileTransferData(
                code = customCode,
                fileName = fileName,
                fileType = fileType,
                fileSize = fileSize,
                fileData = base64Data,
                senderName = senderName,
                timestamp = System.currentTimeMillis()
            )

            val json = adapter.toJson(transferData)
            val requestBody = json.toRequestBody("application/json".toMediaTypeOrNull())
            val url = NetworkClient.getFullUrl(NetworkClient.BUCKET_ID, "transfer_$customCode")
            
            val response = NetworkClient.api.putValue(url, requestBody)
            if (response.isSuccessful) {
                // Update stats
                val prefs = context.getSharedPreferences("alarm_grup_prefs", Context.MODE_PRIVATE)
                val currentCount = prefs.getInt("file_share_send_count", 0)
                prefs.edit().putInt("file_share_send_count", currentCount + 1).apply()
                
                Result.success(customCode)
            } else {
                Result.failure(Exception("Cloud Server menolak unggahan (Respons: ${response.code()})"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Download and decode a shared file using its 6-digit PIN
     */
    suspend fun downloadFile(
        code: String,
        onProgress: (String) -> Unit
    ): Result<File> {
        return try {
            onProgress("Mencari berkas di Cloud...")
            val url = NetworkClient.getFullUrl(NetworkClient.BUCKET_ID, "transfer_$code")
            val response = NetworkClient.api.getValue(url)
            
            if (!response.isSuccessful) {
                return Result.failure(Exception("Cloud Server gagal merespon (Kode: ${response.code()})"))
            }

            val jsonString = response.body()?.string()
            if (jsonString == null || jsonString.trim() == "null") {
                return Result.failure(Exception("Berkas dengan kode PIN $code tidak ditemukan atau sudah kedaluwarsa."))
            }

            onProgress("Mengunduh metadata & isi...")
            val transferData = adapter.fromJson(jsonString)
                ?: return Result.failure(Exception("Format data tidak sesuai atau rusak."))

            onProgress("Mendekode isi berkas...")
            val fileBytes = Base64.decode(transferData.fileData, Base64.DEFAULT)

            // Save locally
            onProgress("Menyimpan di memori lokal...")
            val destFile = File(sharedDir, transferData.fileName)
            destFile.writeBytes(fileBytes)

            // Update stats
            val prefs = context.getSharedPreferences("alarm_grup_prefs", Context.MODE_PRIVATE)
            val currentCount = prefs.getInt("file_share_receive_count", 0)
            prefs.edit().putInt("file_share_receive_count", currentCount + 1).apply()

            Result.success(destFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * List all received or imported files currently saved in the device local storage
     */
    fun getLocalSharedFiles(): List<File> {
        return sharedDir.listFiles()?.toList()?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    /**
     * Delete a local file from storage
     */
    fun deleteLocalFile(file: File): Boolean {
        return if (file.exists()) {
            file.delete()
        } else false
    }

    /**
     * Clean all local sharing cache
     */
    fun clearAllLocalFiles() {
        sharedDir.listFiles()?.forEach { file ->
            file.delete()
        }
    }
}
