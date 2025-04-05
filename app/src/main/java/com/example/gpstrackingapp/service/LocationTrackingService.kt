package com.example.gpstrackingapp.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.example.gpstrackingapp.MainActivity
import com.example.gpstrackingapp.R
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import org.json.JSONArray

class LocationTrackingService : LifecycleService() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val markersList = mutableListOf<LatLng>()
    private var isTracking = false
    private val distanceThreshold = 1f // 100 metre sınırı

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()
        isTracking = true

        // FusedLocationClient ile konum servisi başlatılıyor
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this@LocationTrackingService)

        // Foreground service başlatma
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "location_tracking_channel",
                "Konum Takibi",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Konum güncellemeleri bildirimi"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, "location_tracking_channel")
            .setContentTitle("Konum Takibi")
            .setContentText("Konum izleniyor...")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentIntent(pendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        }

        startLocationUpdates() // Konum güncellemelerini başlat
    }


    private fun startLocationUpdates() {
        Log.e("TAG", "onCreate: help me in updatesmethod", )

        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                Log.d("LocationTrackingService", "onLocationResult çalıştı, isTracking: $isTracking")

                for (location in result.locations) {
                    if (isTracking) {
                        Log.d("LocationTrackingService", "Konum: ${location.latitude}, ${location.longitude}")
                        updateLocation(location)
                    }
                }
            }
        }

        // Konum güncellemelerini başlatıyoruz
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }
    private fun updateLocation(location: Location) {
        val newLatLng = LatLng(location.latitude, location.longitude)

        // Eğer yeterli mesafe varsa, marker ekleyelim
        if (markersList.isEmpty() || getDistanceBetween(markersList.last(), newLatLng) >= distanceThreshold) {
            markersList.add(newLatLng)
            saveMarkers() // Marker'ları kaydet
            sendLocationBroadcast(location) // Broadcast ile konum verilerini gönder
        }
    }


    private fun sendLocationBroadcast(location: Location) {
        val intent = Intent("LOCATION_UPDATE")
        intent.putExtra("latitude", location.latitude)
        intent.putExtra("longitude", location.longitude)
        Log.d("help", "Broadcast gönderiliyor: ${location.latitude}, ${location.longitude}")
        sendBroadcast(intent)
    }


    private fun getDistanceBetween(latLng1: LatLng, latLng2: LatLng): Float {
        val location1 = Location("")
        location1.latitude = latLng1.latitude
        location1.longitude = latLng1.longitude

        val location2 = Location("")
        location2.latitude = latLng2.latitude
        location2.longitude = latLng2.longitude

        return location1.distanceTo(location2)
    }

    private fun saveMarkers() {
        val jsonArray = JSONArray()
        for (marker in markersList) {
            val jsonObject = org.json.JSONObject()
            jsonObject.put("latitude", marker.latitude)
            jsonObject.put("longitude", marker.longitude)
            jsonArray.put(jsonObject)
        }

        val sharedPreferences = getSharedPreferences("MarkersData", MODE_PRIVATE)
        sharedPreferences.edit().putString("markers", jsonArray.toString()).apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback) // Servis sonlandığında konum güncellemelerini durduruyoruz
    }

    override fun onBind(intent: Intent): IBinder? {
        // This service is not bound, so we simply return null.
        return super.onBind(intent)
    }
}
