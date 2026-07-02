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

            // Save locally and automatically handle if ZIP or single file
            val destFile = File(sharedDir, transferData.fileName)
            if (transferData.fileName.endsWith(".zip", ignoreCase = true)) {
                onProgress("Mendekode & mengekstrak file-file (.zip)...")
                destFile.writeBytes(fileBytes)
                unzipFile(destFile, sharedDir, context)
            } else {
                onProgress("Menyimpan di memori lokal...")
                destFile.writeBytes(fileBytes)
                try {
                    onProgress("Menyimpan di folder Unduhan HP...")
                    saveToSystemDownloads(context, fileBytes, transferData.fileName, transferData.fileType)
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }

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

    fun zipUris(uris: List<Uri>, destZipFile: File): Boolean {
        return try {
            java.util.zip.ZipOutputStream(java.io.BufferedOutputStream(destZipFile.outputStream())).use { zos ->
                uris.forEach { uri ->
                    var name = "berkas_${System.currentTimeMillis()}"
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (cursor.moveToFirst() && idx != -1) {
                            name = cursor.getString(idx)
                        }
                    }
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        val entry = java.util.zip.ZipEntry(name)
                        zos.putNextEntry(entry)
                        input.copyTo(zos)
                        zos.closeEntry()
                    }
                }
            }
            true
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
        }
    }

    fun unzipFile(zipFile: File, targetDir: File, context: Context) {
        try {
            java.util.zip.ZipInputStream(java.io.BufferedInputStream(zipFile.inputStream())).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val destFile = File(targetDir, entry.name)
                        destFile.parentFile?.mkdirs()
                        destFile.outputStream().use { fos ->
                            zis.copyTo(fos)
                        }
                        
                        val ext = entry.name.substringAfterLast('.', "")
                        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase()) ?: "application/octet-stream"
                        saveToSystemDownloads(context, destFile.readBytes(), entry.name, mimeType)
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            if (zipFile.exists()) {
                zipFile.delete()
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    fun saveToSystemDownloads(context: Context, fileBytes: ByteArray, fileName: String, mimeType: String): Boolean {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(fileBytes)
                    }
                    true
                } else false
            } else {
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val destFile = File(downloadsDir, fileName)
                destFile.writeBytes(fileBytes)
                true
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
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
