package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.api.NetworkClient
import com.example.data.model.AwakeStatus
import com.example.data.model.CouplePair
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class CoupleWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == ACTION_REFRESH_COUPLE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, CoupleWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (widgetId in allWidgetIds) {
                updateAppWidget(context, appWidgetManager, widgetId)
            }
        } else if (action == ACTION_TOGGLE_AWAKE) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val sharedPrefs = context.getSharedPreferences("alarm_grup_prefs", Context.MODE_PRIVATE)
                    val code = sharedPrefs.getString("joined_group_code", null)
                    val uid = sharedPrefs.getString("user_id", null)
                    val name = sharedPrefs.getString("user_name", "Anonim") ?: "Anonim"
                    val coupleJson = sharedPrefs.getString("active_couple_json", null)

                    if (code != null && uid != null) {
                        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                        
                        // 1. Post our awake status as true
                        val status = AwakeStatus(
                            userId = uid,
                            nickname = name,
                            isAwake = true,
                            timestamp = System.currentTimeMillis()
                        )
                        val statusAdapter = moshi.adapter(AwakeStatus::class.java)
                        val statusJson = statusAdapter.toJson(status)
                        val statusBody = statusJson.toRequestBody("application/json".toMediaTypeOrNull())

                        val url = NetworkClient.getFullUrl(NetworkClient.BUCKET_ID, "awake_statuses_${code}/$uid")
                        val response = NetworkClient.api.putValue(url, statusBody)

                        if (response.isSuccessful && coupleJson != null) {
                            // 2. Fetch or update couple pairs with scoring
                            try {
                                val pairAdapter = moshi.adapter(CouplePair::class.java)
                                val activePair = pairAdapter.fromJson(coupleJson)
                                if (activePair != null) {
                                    val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                                    val cal = java.util.Calendar.getInstance()
                                    cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
                                    val yesterdayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.time)

                                    val isPartnerA = activePair.partnerA == uid
                                    val lastWakeSelf = if (isPartnerA) activePair.lastWakeA else activePair.lastWakeB

                                    if (lastWakeSelf != todayStr) {
                                        // Simple default scoring on widget click: +5 base points
                                        val ptsToAdd = 5
                                        val currentStreak = if (isPartnerA) activePair.streakA else activePair.streakB
                                        val newStreak = when (lastWakeSelf) {
                                            yesterdayStr -> currentStreak + 1
                                            todayStr -> currentStreak
                                            else -> 1
                                        }

                                        val systemNewDayReset = activePair.lastWakeA != todayStr && activePair.lastWakeB != todayStr
                                        val newSyncBonus = if (systemNewDayReset) false else activePair.syncBonusToday

                                        val updatedPair = if (isPartnerA) {
                                            activePair.copy(
                                                scoreA = activePair.scoreA + ptsToAdd,
                                                streakA = newStreak,
                                                lastWakeA = todayStr,
                                                syncBonusToday = newSyncBonus
                                            )
                                        } else {
                                            activePair.copy(
                                                scoreB = activePair.scoreB + ptsToAdd,
                                                streakB = newStreak,
                                                lastWakeB = todayStr,
                                                syncBonusToday = newSyncBonus
                                            )
                                        }

                                        val pairUrl = NetworkClient.getFullUrl(NetworkClient.BUCKET_ID, "couple_pairs_${code}/${activePair.id}")
                                        val updatedPairJson = pairAdapter.toJson(updatedPair)
                                        val pairBody = updatedPairJson.toRequestBody("application/json".toMediaTypeOrNull())
                                        
                                        val coupleResponse = NetworkClient.api.putValue(pairUrl, pairBody)
                                        if (coupleResponse.isSuccessful) {
                                            sharedPrefs.edit().putString("active_couple_json", updatedPairJson).apply()
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        // Trigger widget reload
                        val appWidgetManager = AppWidgetManager.getInstance(context)
                        val thisWidget = ComponentName(context, CoupleWidgetProvider::class.java)
                        val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
                        for (widgetId in allWidgetIds) {
                            updateAppWidget(context, appWidgetManager, widgetId)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        const val ACTION_REFRESH_COUPLE = "com.example.widget.ACTION_REFRESH_COUPLE"
        const val ACTION_TOGGLE_AWAKE = "com.example.widget.ACTION_TOGGLE_AWAKE"

        fun triggerUpdate(context: Context) {
            val intent = Intent(context, CoupleWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_COUPLE
            }
            context.sendBroadcast(intent)
        }

        private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val sharedPrefs = context.getSharedPreferences("alarm_grup_prefs", Context.MODE_PRIVATE)
            val coupleJson = sharedPrefs.getString("active_couple_json", null)
            val myUid = sharedPrefs.getString("user_id", "") ?: ""

            val views = RemoteViews(context.packageName, R.layout.couple_widget)

            if (coupleJson == null) {
                // Show "No couple" view
                views.setViewVisibility(R.id.no_couple_layout, View.VISIBLE)
                views.setViewVisibility(R.id.couple_active_layout, View.GONE)
            } else {
                // Show "Active couple" layout
                views.setViewVisibility(R.id.no_couple_layout, View.GONE)
                views.setViewVisibility(R.id.couple_active_layout, View.VISIBLE)

                try {
                    val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                    val pairAdapter = moshi.adapter(CouplePair::class.java)
                    val activePair = pairAdapter.fromJson(coupleJson)

                    if (activePair != null) {
                        val isPartnerA = activePair.partnerA == myUid
                        
                        // Parse names & display details
                        val nameA = if (isPartnerA) "${activePair.partnerAName} (Anda)" else activePair.partnerAName
                        val nameB = if (!isPartnerA) "${activePair.partnerBName} (Anda)" else activePair.partnerBName

                        views.setTextViewText(R.id.partner_a_name, nameA)
                        views.setTextViewText(R.id.partner_b_name, nameB)

                        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                        val statusA = if (activePair.lastWakeA == todayStr) "☀️ Sudah Bangun" else "💤 Masih Tidur"
                        val statusB = if (activePair.lastWakeB == todayStr) "☀️ Sudah Bangun" else "💤 Masih Tidur"

                        views.setTextViewText(R.id.partner_a_status, statusA)
                        views.setTextViewText(R.id.partner_b_status, statusB)

                        views.setTextViewText(R.id.partner_a_stats, "Poin: ${activePair.scoreA} | Streak: ${activePair.streakA} 🔥")
                        views.setTextViewText(R.id.partner_b_stats, "Poin: ${activePair.scoreB} | Streak: ${activePair.streakB} 🔥")

                        // Default battery status
                        views.setTextViewText(R.id.partner_a_battery, "🔋 ?%")
                        views.setTextViewText(R.id.partner_b_battery, "🔋 ?%")

                        // Sync bonus indicator
                        if (activePair.syncBonusToday) {
                            views.setViewVisibility(R.id.sync_bonus_indicator, View.VISIBLE)
                        } else {
                            views.setViewVisibility(R.id.sync_bonus_indicator, View.GONE)
                        }

                        // Determine if we can wake up
                        val myLastWake = if (isPartnerA) activePair.lastWakeA else activePair.lastWakeB
                        if (myLastWake == todayStr) {
                            views.setTextViewText(R.id.widget_toggle_awake, "SAYA SUDAH BANGUN ☀️")
                            views.setViewVisibility(R.id.widget_toggle_awake, View.GONE) // Hide if already awake
                        } else {
                            views.setViewVisibility(R.id.widget_toggle_awake, View.VISIBLE)
                            views.setTextViewText(R.id.widget_toggle_awake, "SAYA SUDAH BANGUN ☀️")
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Click on container opens the app
            val clickIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                100,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            // Click on toggle button triggers wake up status update
            val toggleIntent = Intent(context, CoupleWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_AWAKE
            }
            val pendingToggle = PendingIntent.getBroadcast(
                context,
                101,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_toggle_awake, pendingToggle)

            appWidgetManager.updateAppWidget(appWidgetId, views)

            if (coupleJson != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val code = sharedPrefs.getString("joined_group_code", null) ?: return@launch
                        val url = NetworkClient.getFullUrl(NetworkClient.BUCKET_ID, "members_$code")
                        val response = NetworkClient.api.getValue(url)
                        if (response.isSuccessful) {
                            val membersJson = response.body()?.string()
                            if (membersJson != null) {
                                val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                                val mapType = com.squareup.moshi.Types.newParameterizedType(Map::class.java, String::class.java, com.example.data.model.MemberData::class.java)
                                val adapter = moshi.adapter<Map<String, com.example.data.model.MemberData>>(mapType)
                                val members = adapter.fromJson(membersJson)

                                if (members != null) {
                                    val pairAdapter = moshi.adapter(CouplePair::class.java)
                                    val activePair = pairAdapter.fromJson(coupleJson)
                                    if (activePair != null) {
                                        val memberA = members[activePair.partnerA]
                                        val memberB = members[activePair.partnerB]

                                        val updatedViews = RemoteViews(context.packageName, R.layout.couple_widget)
                                        
                                        val batA = memberA?.batteryLevel
                                        val batTextA = if (batA != null) {
                                            if (batA > 20) "🔋 $batA%" else "🪫 $batA%"
                                        } else "🔋 ?%"
                                        updatedViews.setTextViewText(R.id.partner_a_battery, batTextA)

                                        val batB = memberB?.batteryLevel
                                        val batTextB = if (batB != null) {
                                            if (batB > 20) "🔋 $batB%" else "🪫 $batB%"
                                        } else "🔋 ?%"
                                        updatedViews.setTextViewText(R.id.partner_b_battery, batTextB)
                                        
                                        // Ensure other parts of the view remain up to date
                                        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, updatedViews)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}
