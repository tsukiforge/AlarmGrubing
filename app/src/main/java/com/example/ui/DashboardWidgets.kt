package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.ui.theme.TextLight
import com.example.ui.theme.TextMuted
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

@Composable
fun MotivationalQuoteWidget() {
    val quotes = listOf(
        "Setiap pagi adalah kesempatan baru untuk menjadi lebih baik.",
        "Bangunlah dengan tekad, tidurlah dengan kepuasan.",
        "Hari ini adalah awal dari sesuatu yang luar biasa.",
        "Jangan tunggu hari yang sempurna, jadikan hari ini sempurna.",
        "Mimpi hanya akan menjadi kenyataan jika kamu bangun untuk mewujudkannya.",
        "Tersenyumlah, karena hari ini penuh dengan peluang baru.",
        "Kunci kesuksesan adalah bangun lebih awal dan bekerja lebih keras."
    )
    
    val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    val quote = quotes[currentDay % quotes.size]
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.FormatQuote, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Motivasi Pagi ☀️", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "\"$quote\"", fontSize = 13.sp, fontStyle = FontStyle.Italic, color = TextLight, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
fun MorningWeatherWidget() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var weatherDesc by remember { mutableStateOf("Memuat Cuaca...") }
    var temperature by remember { mutableStateOf("--°C") }
    var locationName by remember { mutableStateOf("Mencari lokasi...") }
    var isRaining by remember { mutableStateOf(false) }
    var isCloudy by remember { mutableStateOf(false) }
    var isSunny by remember { mutableStateOf(true) }
    var hasPermission by remember { 
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) 
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            weatherDesc = "Izin lokasi diperlukan"
            locationName = "Ketuk untuk izinkan"
        } else {
            weatherDesc = "Menyinkronkan awan..."
            locationName = "Lokasi Anda"
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
                    if (loc != null) {
                        scope.launch {
                            val lat = loc.latitude
                            val lon = loc.longitude
                            try {
                                val weatherInfo = fetchWeather(lat, lon)
                                temperature = "${weatherInfo.first}°C"
                                val code = weatherInfo.second
                                // Simple WMO code mapping
                                when {
                                    code in 50..69 || code in 80..82 || code in 95..99 -> {
                                        isRaining = true
                                        isSunny = false
                                        isCloudy = false
                                        weatherDesc = "Hujan"
                                    }
                                    code in 1..3 || code in 45..48 -> {
                                        isCloudy = true
                                        isSunny = false
                                        isRaining = false
                                        weatherDesc = if (code in 45..48) "Berkabut" else "Berawan"
                                    }
                                    else -> {
                                        isSunny = true
                                        isCloudy = false
                                        isRaining = false
                                        weatherDesc = "Cerah"
                                    }
                                }
                            } catch (e: Exception) {
                                weatherDesc = "Gagal memuat cuaca"
                            }
                        }
                    } else {
                        weatherDesc = "Lokasi tidak ditemukan"
                    }
                }
            } catch (e: SecurityException) {
                weatherDesc = "Akses lokasi ditolak"
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(16.dp),
        onClick = {
            if (!hasPermission) {
                permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(100.dp)) {
            // Background Image
            val bgRes = when {
                isRaining -> R.drawable.weather_rainy_1782545360489
                isCloudy -> R.drawable.weather_cloudy_1782545347634
                else -> R.drawable.weather_sunny_1782545330091
            }
            Image(
                painter = painterResource(id = bgRes),
                contentDescription = "Cuaca Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // Overlay gradient for readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )
            
            Row(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(locationName, fontSize = 12.sp, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(weatherDesc, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Text(temperature, fontSize = 36.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
        }
    }
}

suspend fun fetchWeather(lat: Double, lon: Double): Pair<Int, Int> = withContext(Dispatchers.IO) {
    val urlStr = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true"
    val url = URL(urlStr)
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "GET"
    conn.connectTimeout = 5000
    conn.readTimeout = 5000
    
    val response = conn.inputStream.bufferedReader().use { it.readText() }
    val json = JSONObject(response)
    val current = json.getJSONObject("current_weather")
    val temp = current.getDouble("temperature").toInt()
    val code = current.getInt("weathercode")
    Pair(temp, code)
}
