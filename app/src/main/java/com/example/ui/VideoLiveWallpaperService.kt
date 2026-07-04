package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

// Konstanta sebagai top-level private — menghindari companion object restriction di beberapa versi Kotlin
private const val VLW_TAG = "VideoLiveWallpaper"
private const val VLW_PREFS_NAME = "wallpaper_prefs"
private const val VLW_KEY_VIDEO_URI = "video_uri"
private const val VLW_BATTERY_LOW_THRESHOLD = 15

/**
 * VideoLiveWallpaperService — Live Wallpaper berbasis video menggunakan ExoPlayer.
 *
 * Arsitektur:
 * - WallpaperService adalah service khusus Android yang berjalan terus-menerus di background.
 * - Setiap instance wallpaper (bisa ada lebih dari satu: preview + aktif) menggunakan
 *   Engine (inner class) yang punya SurfaceHolder sendiri.
 * - ExoPlayer me-render frame video langsung ke SurfaceHolder.
 *
 * Pertimbangan performa & baterai:
 * - Video di-pause otomatis saat:
 *   a) Surface tidak terlihat (onVisibilityChanged false)
 *   b) Level baterai ≤ 15% (battery saver mode)
 * - Video di-loop tanpa suara (setVolume 0).
 * - ExoPlayer di-release saat Engine di-destroy untuk mencegah memory leak.
 *
 * Cara set video:
 * Simpan URI string ke SharedPreferences "wallpaper_prefs" key "video_uri"
 * sebelum engine dibuat. Atau gunakan WallpaperHelper.openVideoLiveWallpaperPicker()
 * yang akan membuka system wallpaper picker.
 */
class VideoLiveWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = VideoEngine()

    inner class VideoEngine : Engine() {

        private var player: ExoPlayer? = null
        private val mainHandler = Handler(Looper.getMainLooper())

        // Track visibility untuk suspend playback saat layar di-cover / app di foreground
        private var isSurfaceVisible = false

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            Log.d(VLW_TAG, "Engine onCreate")
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            Log.d(VLW_TAG, "Surface created — inisialisasi ExoPlayer")
            initPlayer(holder)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            // ExoPlayer handle resize secara otomatis via Surface yang sama
        }

        override fun onVisibilityChanged(visible: Boolean) {
            isSurfaceVisible = visible
            if (visible) {
                if (!isBatteryLow()) {
                    player?.play()
                    Log.d(VLW_TAG, "Wallpaper terlihat — video di-resume")
                } else {
                    Log.d(VLW_TAG, "Wallpaper terlihat tapi baterai rendah — video tetap pause")
                }
            } else {
                player?.pause()
                Log.d(VLW_TAG, "Wallpaper tidak terlihat — video di-pause")
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            Log.d(VLW_TAG, "Surface destroyed — release ExoPlayer")
            releasePlayer()
        }

        override fun onDestroy() {
            super.onDestroy()
            releasePlayer()
        }

        private fun initPlayer(holder: SurfaceHolder) {
            // Jalankan di main thread karena ExoPlayer harus diinisialisasi di main thread
            mainHandler.post {
                try {
                    val prefs = getSharedPreferences(VLW_PREFS_NAME, Context.MODE_PRIVATE)
                    val videoUriString = prefs.getString(VLW_KEY_VIDEO_URI, null)

                    if (videoUriString == null) {
                        Log.w(VLW_TAG, "Tidak ada URI video yang disimpan di prefs. Live wallpaper tampil hitam.")
                        return@post
                    }

                    val videoUri = Uri.parse(videoUriString)

                    // Validasi URI sebelum diberikan ke ExoPlayer
                    try {
                        contentResolver.openInputStream(videoUri)?.close()
                    } catch (e: Exception) {
                        Log.e(VLW_TAG, "Video URI tidak bisa dibaca: $videoUriString — ${e.message}")
                        return@post
                    }

                    val exoPlayer = ExoPlayer.Builder(applicationContext).build().apply {
                        setMediaItem(MediaItem.fromUri(videoUri))
                        repeatMode = Player.REPEAT_MODE_ALL   // Loop terus-menerus
                        val isMuted = prefs.getBoolean("video_muted", false)
                        volume = if (isMuted) 0f else 1f
                        setVideoSurface(holder.surface)
                        prepare()
                        addListener(object : Player.Listener {
                            override fun onPlaybackStateChanged(playbackState: Int) {
                                when (playbackState) {
                                    Player.STATE_READY -> {
                                        Log.d(VLW_TAG, "ExoPlayer READY — duration=${duration}ms")
                                        if (isSurfaceVisible && !isBatteryLow()) play()
                                    }
                                    Player.STATE_ENDED -> Log.d(VLW_TAG, "Playback ended (seharusnya loop)")
                                    Player.STATE_IDLE -> Log.d(VLW_TAG, "ExoPlayer IDLE")
                                    Player.STATE_BUFFERING -> Log.d(VLW_TAG, "ExoPlayer BUFFERING")
                                }
                            }
                            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                                Log.e(VLW_TAG, "ExoPlayer error: ${error.message}", error)
                            }
                        })
                    }

                    player = exoPlayer
                    Log.d(VLW_TAG, "ExoPlayer berhasil diinisialisasi untuk URI: $videoUriString")

                } catch (e: Exception) {
                    Log.e(VLW_TAG, "Gagal inisialisasi ExoPlayer: ${e.message}", e)
                }
            }
        }

        private fun releasePlayer() {
            mainHandler.post {
                player?.let {
                    it.stop()
                    it.release()
                    Log.d(VLW_TAG, "ExoPlayer di-release")
                }
                player = null
            }
        }

        /**
         * Cek apakah baterai sedang dalam kondisi rendah (≤ 15%).
         * Jika ya, playback video di-pause untuk menghemat daya.
         */
        private fun isBatteryLow(): Boolean {
            return try {
                val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                val isLow = level <= VLW_BATTERY_LOW_THRESHOLD
                if (isLow) Log.d(VLW_TAG, "Baterai rendah ($level%) — video wallpaper di-pause")
                isLow
            } catch (e: Exception) {
                false // Default: anggap baterai cukup jika tidak bisa dibaca
            }
        }
    }
}
