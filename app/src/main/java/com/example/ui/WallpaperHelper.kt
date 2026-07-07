package com.example.ui

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WallpaperHelper — utilitas untuk set wallpaper statis dari asset aplikasi.
 *
 * Untuk wallpaper VIDEO, gunakan VideoLiveWallpaperService (live wallpaper)
 * yang didaftarkan di manifest, bukan helper ini.
 */
object WallpaperHelper {

    private const val TAG = "WallpaperHelper"

    sealed class WallpaperResult {
        object Success : WallpaperResult()
        data class Error(val message: String) : WallpaperResult()
    }

    /**
     * Set wallpaper statis dari content URI (gambar dari galeri / file picker).
     * Harus dipanggil dari coroutine (suspend) karena operasi I/O.
     *
     * @param context  Application context
     * @param imageUri Content URI gambar yang sudah memiliki persistent read permission
     * @return         [WallpaperResult.Success] atau [WallpaperResult.Error]
     */
    suspend fun setWallpaperFromUri(context: Context, imageUri: Uri, flag: Int? = null): WallpaperResult =
        withContext(Dispatchers.IO) {
            try {
                if (!imageUri.toString().contains("previous_wallpaper.jpg")) {
                    saveCurrentWallpaperAsPrevious(context)
                }
                val wallpaperManager = WallpaperManager.getInstance(context)
                context.contentResolver.openInputStream(imageUri)?.use { stream ->
                    if (flag != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        wallpaperManager.setStream(stream, null, true, flag)
                    } else {
                        wallpaperManager.setStream(stream)
                    }
                    Log.d(TAG, "Wallpaper statis berhasil di-set dari URI: $imageUri")
                    WallpaperResult.Success
                } ?: WallpaperResult.Error("Tidak bisa membuka file gambar")
            } catch (e: SecurityException) {
                Log.e(TAG, "Permission ditolak untuk URI: $imageUri — ${e.message}")
                WallpaperResult.Error("Izin akses file ditolak. Coba pilih gambar ulang.")
            } catch (e: Exception) {
                Log.e(TAG, "Gagal set wallpaper: ${e.message}", e)
                WallpaperResult.Error("Gagal set wallpaper: ${e.message}")
            }
        }

    /**
     * Set wallpaper statis dari drawable resource (template bawaan aplikasi).
     *
     * @param context    Application context
     * @param drawableId Resource ID drawable
     */
    suspend fun setWallpaperFromResource(context: Context, drawableId: Int): WallpaperResult =
        withContext(Dispatchers.IO) {
            try {
                saveCurrentWallpaperAsPrevious(context)
                val wallpaperManager = WallpaperManager.getInstance(context)
                context.resources.openRawResource(drawableId).use { stream ->
                    wallpaperManager.setStream(stream)
                }
                Log.d(TAG, "Wallpaper statis berhasil di-set dari resource: $drawableId")
                WallpaperResult.Success
            } catch (e: Exception) {
                Log.e(TAG, "Gagal set wallpaper dari resource: ${e.message}", e)
                WallpaperResult.Error("Gagal set wallpaper: ${e.message}")
            }
        }

    fun ensurePreviousWallpaperSaved(context: Context) {
        val file = java.io.File(context.filesDir, "previous_wallpaper.jpg")
        if (!file.exists()) {
            saveCurrentWallpaperAsPrevious(context)
        }
    }

    fun saveCurrentWallpaperAsPrevious(context: Context) {
        try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            val drawable = wallpaperManager.drawable
            if (drawable != null) {
                val bitmap = drawableToBitmap(drawable)
                val file = java.io.File(context.filesDir, "previous_wallpaper.jpg")
                java.io.FileOutputStream(file).use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                }
                Log.d(TAG, "Wallpaper sebelumnya berhasil dicadangkan ke: ${file.absolutePath}")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Gagal menyimpan wallpaper sebelumnya karena masalah izin: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menyimpan wallpaper sebelumnya: ${e.message}")
        }
    }

    private fun drawableToBitmap(drawable: android.graphics.drawable.Drawable): android.graphics.Bitmap {
        if (drawable is android.graphics.drawable.BitmapDrawable) {
            if (drawable.bitmap != null) {
                return drawable.bitmap
            }
        }
        val bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)
        } else {
            android.graphics.Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, android.graphics.Bitmap.Config.ARGB_8888)
        }
        val canvas = android.graphics.Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    /**
     * Buka sistem Live Wallpaper picker yang mengarah ke VideoLiveWallpaperService.
     * User akan diperlihatkan preview dan tombol "Set Wallpaper" dari sistem.
     *
     * @param context Activity context (butuh startActivity)
     */
    fun openVideoLiveWallpaperPicker(context: Context) {
        try {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    android.content.ComponentName(
                        context.packageName,
                        VideoLiveWallpaperService::class.java.name
                    )
                )
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Gagal membuka live wallpaper picker: ${e.message}", e)
        }
    }
}
