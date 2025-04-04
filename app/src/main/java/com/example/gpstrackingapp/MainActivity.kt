package com.example.gpstrackingapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import org.json.JSONArray
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var lastLocation: Location? = null
    private var distanceThreshold = 100f // 100 metre sınırı
    private val markersList = mutableListOf<LatLng>()
    private var isFirstLocation = true
    private var isTracking = false // Takip durumu
    private lateinit var startStopButton: Button // Buton değişkeni
    private lateinit var resetButton: Button // Sıfırla butonu değişkeni

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Buton referansları
        startStopButton = findViewById(R.id.startStopButton)
        resetButton = findViewById(R.id.clearButton)

        // Butona tıklama işlevi
        startStopButton.setOnClickListener {
            toggleTracking() // Takip başlat/durdur
        }

        // "Sıfırla" butonuna tıklama işlevi
        resetButton.setOnClickListener {
            resetMarkers() // Marker'ları sıfırla
        }

        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    if (isTracking) { // Takip aktifse marker eklenir
                        updateLocation(location)
                    }
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun updateLocation(location: Location) {
        val newLatLng = LatLng(location.latitude, location.longitude)

        if (isFirstLocation) {
            googleMap.addMarker(
                MarkerOptions().position(newLatLng)
                    .title("Başlangıç Konumu")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(newLatLng, 15f))
            isFirstLocation = false
        } else {
            val distance = lastLocation?.distanceTo(location) ?: 0f
            if (distance >= distanceThreshold) {
                googleMap.addMarker(MarkerOptions().position(newLatLng).title("Konum Noktası"))
                markersList.add(newLatLng)
                saveMarkers()
            }
        }

        // Konum her güncellendiğinde, haritaya zoom yapalım
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(newLatLng, 15f))

        lastLocation = location
    }

    private fun saveMarkers() {
        val jsonArray = JSONArray()
        for (marker in markersList) {
            val jsonObject = org.json.JSONObject()
            jsonObject.put("latitude", marker.latitude)
            jsonObject.put("longitude", marker.longitude)
            jsonArray.put(jsonObject)
        }

        val sharedPreferences = getSharedPreferences("MarkersData", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("markers", jsonArray.toString()).apply()
    }

    private fun loadMarkers() {
        val sharedPreferences = getSharedPreferences("MarkersData", Context.MODE_PRIVATE)
        val jsonString = sharedPreferences.getString("markers", null)

        if (!jsonString.isNullOrEmpty()) {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val lat = jsonObject.getDouble("latitude")
                val lng = jsonObject.getDouble("longitude")
                val latLng = LatLng(lat, lng)

                googleMap.addMarker(MarkerOptions().position(latLng).title("Önceki Nokta"))
                markersList.add(latLng)
            }
        }
    }

    private fun toggleTracking() {
        if (isTracking) {
            stopLocationUpdates() // Takip durduruluyor
            startStopButton.text = "Başlat" // Butonun metni değişiyor
        } else {
            startLocationUpdates() // Takip başlatılıyor
            startStopButton.text = "Durdur" // Butonun metni değişiyor
        }
        isTracking = !isTracking // Takip durumu değişiyor
    }

    // Marker'ları sıfırlamak için kullanılan fonksiyon
    private fun resetMarkers() {
        googleMap.clear() // Tüm marker'ları haritadan sil
        markersList.clear() // Listeden marker'ları temizle
        val sharedPreferences = getSharedPreferences("MarkersData", Context.MODE_PRIVATE)
        sharedPreferences.edit().remove("markers").apply()
        Toast.makeText(this, "Tüm noktalar sıfırlandı", Toast.LENGTH_SHORT).show()
    }

    // Marker'a tıklanınca adres bilgisini gösteren fonksiyon
    private fun showAddressInfo(latLng: LatLng) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addressList = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (addressList != null && addressList.isNotEmpty()) {
                val address = addressList[0]
                val addressInfo = "Adres: ${address.getAddressLine(0)}"
                Toast.makeText(this, addressInfo, Toast.LENGTH_LONG).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Adres bilgisi alınamadı", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.isMyLocationEnabled = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        loadMarkers()

        // Marker tıklama olayı
        googleMap.setOnMarkerClickListener { marker ->
            val latLng = marker.position
            showAddressInfo(latLng) // Tıklanan marker'ın adresini göster
            true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        } else {
            Toast.makeText(this, "Konum izni gerekli!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
    override fun onDestroy() { super.onDestroy(); mapView.onDestroy() }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }
}
