package com.example.gpstrackingapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import org.json.JSONArray

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var lastLocation: Location? = null
    private var distanceThreshold = 100f // 100 metre sınırı
    private val markersList = mutableListOf<LatLng>()
    private var isFirstLocation = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

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
                    updateLocation(location)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
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

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.isMyLocationEnabled = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        loadMarkers()
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
