package com.example.gpstrackingapp

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.gpstrackingapp.service.LocationTrackingService
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.MapView
import org.json.JSONArray

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private lateinit var startStopButton: Button
    private lateinit var resetButton: Button

    private val markersList = mutableListOf<LatLng>()

    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("help", "Broadcast alındı")

            val latitude = intent?.getDoubleExtra("latitude", 0.0)
            val longitude = intent?.getDoubleExtra("longitude", 0.0)

            if (latitude != null && longitude != null) {
                val newLatLng = LatLng(latitude, longitude)
                Log.d("help", "Yeni konum alındı: $latitude, $longitude")

                // googleMap hazır mı kontrolü
                if (::googleMap.isInitialized) {
                    googleMap.addMarker(
                        MarkerOptions().position(newLatLng).title("Yeni Nokta")
                    )
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(newLatLng, 17f))
                    markersList.add(newLatLng)
                    saveMarkersToPrefs()
                } else {
                    Log.e("help", "Harita henüz hazır değil, marker eklenemedi")
                }
            } else {
                Log.e("help", "Konum verisi alınamadı")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d("help", "onCreate çalıştı")

        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        startStopButton = findViewById(R.id.startStopButton)
        resetButton = findViewById(R.id.clearButton)
        startLocationUpdates()

        startStopButton.setOnClickListener { toggleTracking() }
        resetButton.setOnClickListener { resetMarkers() }

        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }

    private fun startLocationUpdates() {
        val serviceIntent = Intent(this, LocationTrackingService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun stopLocationUpdates() {
        stopService(Intent(this, LocationTrackingService::class.java))
    }

    private fun toggleTracking() {
        if (startStopButton.text == "Başlat") {
            Log.d("help", "Takip başlatıldı")
            startStopButton.text = "Durdur"
        } else {
            Log.d("help", "Takip durduruldu")
            stopLocationUpdates()
            startStopButton.text = "Başlat"
        }
    }

    private fun resetMarkers() {
        Log.d("help", "Noktalar sıfırlanıyor")
        googleMap.clear()
        markersList.clear()
        val sharedPreferences = getSharedPreferences("MarkersData", Context.MODE_PRIVATE)
        sharedPreferences.edit().remove("markers").apply()
        Toast.makeText(this, "Tüm noktalar sıfırlandı", Toast.LENGTH_SHORT).show()
    }

    override fun onMapReady(map: GoogleMap) {
        Log.d("help", "Harita hazır")
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true

        // Konum izni kontrolü ve harita üzerinde kullanıcının konumunu göster
        googleMap.isMyLocationEnabled =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        // "My Location" butonunun görünür olmasını sağlar
        googleMap.uiSettings.isMyLocationButtonEnabled = true

        loadMarkers()
    }

    private fun saveMarkersToPrefs() {
        val sharedPreferences = getSharedPreferences("MarkersData", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        val jsonArray = JSONArray()
        for (latLng in markersList) {
            val jsonObject = org.json.JSONObject()
            jsonObject.put("latitude", latLng.latitude)
            jsonObject.put("longitude", latLng.longitude)
            jsonArray.put(jsonObject)
        }

        editor.putString("markers", jsonArray.toString())
        editor.apply()
    }

    private fun loadMarkers() {
        Log.d("help", "Kayıtlı marker'lar yükleniyor")
        val sharedPreferences = getSharedPreferences("MarkersData", Context.MODE_PRIVATE)
        val jsonString = sharedPreferences.getString("markers", null)

        if (!jsonString.isNullOrEmpty()) {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val lat = jsonObject.getDouble("latitude")
                val lng = jsonObject.getDouble("longitude")
                val latLng = LatLng(lat, lng)

                val title = if (i == 0) "Başlangıç Noktası" else "Önceki Nokta"
                googleMap.addMarker(MarkerOptions().position(latLng).title(title))
                markersList.add(latLng)

                if (i == 0) {
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter("LOCATION_UPDATE")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.d("help", "onResume çalıştı, receiver kaydediliyor")

            // Android 13 ve sonrasında RECEIVER_NOT_EXPORTED zorunlu
            registerReceiver(locationReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            // Önceki sürümlerde bu parametre gerekmez
            registerReceiver(locationReceiver, filter)
        }

        Log.d("help", "Receiver kaydedildi")
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        Log.d("help", "onPause: Receiver kaldırılıyor")
        unregisterReceiver(locationReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
        Log.d("help", "onDestroy")
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}
