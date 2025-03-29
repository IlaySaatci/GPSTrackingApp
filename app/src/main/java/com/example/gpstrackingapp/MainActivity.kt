package com.example.gpstrackingapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.gpstrackingapp.viewmodel.ActivityViewModel

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private var mapView: MapView? = null
    private var googleMap: GoogleMap? = null

    // ViewModel'i bağlayalım
    private val viewModel: ActivityViewModel by viewModels()

    // Konum iznini almak için launcher
    private val locationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // İzin verildi, haritayı başlat
                initMapView()
            } else {
                // İzin verilmedi, kullanıcıya bilgi ver
                Toast.makeText(this, "Konum izni gerekli", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Konum iznini kontrol et ve izin istenirse, harita başlat
        checkLocationPermission()
    }

    // Konum izni kontrolü
    private fun checkLocationPermission() {
        val permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)

        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            // İzin verilmişse haritayı başlat
            initMapView()
        } else {
            // İzin verilmemişse izin iste
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Harita başlatma işlemi
    private fun initMapView() {
        // Harita başlatma işlemi
        mapView = findViewById(R.id.mapView)
        mapView?.onCreate(null)
        mapView?.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Konum verisini kullanabilmek için izin verilmişse
        googleMap?.isMyLocationEnabled = true

        // Örnek İstanbul koordinatları
        val istanbul = LatLng(41.0082, 28.9784)

        // Örnek Marker (İstanbul)
        googleMap?.addMarker(MarkerOptions().position(istanbul).title("İstanbul"))
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(istanbul, 12f))
    }

    // MapView Yaşam Döngüsü Metotları
    override fun onResume() {
        super.onResume()
        mapView?.onResume()  // null güvenliği ile mapView çağrılabilir
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }
}
