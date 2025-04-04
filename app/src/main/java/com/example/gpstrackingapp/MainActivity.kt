package com.example.gpstrackingapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.gpstrackingapp.viewmodel.ActivityViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import java.util.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private var mapView: MapView? = null
    private var googleMap: GoogleMap? = null
    private val mainActivityViewModel: ActivityViewModel by viewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastLocation: Location? = null
    private var distanceTraveled = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Konum izni kontrolü
        checkLocationPermission()

        // MapView başlatma
        initMapView()

        // LiveData'yı gözlemleme
        mainActivityViewModel.markersAdded.observe(this, { isAdded ->
            if (isAdded) {
                Toast.makeText(this, "Markerlar eklendi!", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun checkLocationPermission() {
        val permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)

        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            // İzin verilmişse haritayı başlat
            startLocationUpdates()
        } else {
            // İzin verilmemişse izin isteyin
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        // Konum güncellemelerini almak için LocationRequest oluşturuyoruz
        val locationRequest = LocationRequest.create().apply {
            interval = 10000 // Her 10 saniyede bir konum güncellenmesi
            fastestInterval = 5000 // En hızlı interval
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY // Yüksek doğruluk
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)
                p0?.let {
                    val location = it.lastLocation
                    // Eğer lastLocation null değilse, updateMarkers fonksiyonuna geçelim
                    location?.let { newLocation ->
                        updateMarkers(LatLng(newLocation.latitude, newLocation.longitude))
                    }
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun initMapView() {
        // MapView'in başlatılması
        mapView = findViewById(R.id.mapView)
        mapView?.onCreate(null)
        mapView?.getMapAsync(this) // Harita hazır olduğunda onMapReady çağrılır
    }

    private var markerCount = 0

    // Marker eklemek için kullanılan fonksiyon
    private fun updateMarkers(newLocation: LatLng) {
        if (lastLocation == null) {
            // İlk konum, yani başlangıç noktası
            lastLocation = Location("provider").apply {
                latitude = newLocation.latitude
                longitude = newLocation.longitude
            }
            return // İlk güncellemede marker eklemeden çıkıyoruz
        }

        // Son konum ile yeni konum arasındaki mesafeyi hesapla
        val distance = lastLocation!!.distanceTo(Location("provider").apply {
            latitude = newLocation.latitude
            longitude = newLocation.longitude
        })

        // Eğer mesafe 100 metreden fazla ise yeni marker ekle
        if (distance >= 100) {
            // Haritaya marker ekleyin
            googleMap?.addMarker(
                MarkerOptions()
                    .position(newLocation)
                    .title("Marker #${++markerCount}")
            )

            // Son konumu güncelle
            lastLocation = Location("provider").apply {
                latitude = newLocation.latitude
                longitude = newLocation.longitude
            }
        }
    }

    private fun onMarkerClick(latLng: LatLng) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            // Geocoder ile adres bilgilerini alıyoruz
            val addresses: List<Address>? = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)

            // Eğer adres bilgisi varsa
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val addressString = StringBuilder()

                // Adresin bilgilerini alıyoruz
                addressString.append(address.getAddressLine(0)) // Ana adres
                addressString.append(", ")
                addressString.append(address.locality) // Şehir
                addressString.append(", ")
                addressString.append(address.countryName) // Ülke

                // Adres bilgisini Toast ile gösteriyoruz
                Toast.makeText(this, "Adres: $addressString", Toast.LENGTH_LONG).show()
            } else {
                // Eğer adres bulunamazsa
                Toast.makeText(this, "Adres bulunamadı.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            // Hata durumunda kullanıcıyı bilgilendiriyoruz
            Toast.makeText(this, "Adres alınırken hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // MapView Yaşam Döngüsü Metotları
    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Konum verisini kullanabilmek için izin verilmişse
        googleMap?.isMyLocationEnabled = true

        // Haritada marker'a tıklama olayını işlemek için
        googleMap?.setOnMarkerClickListener { marker ->
            // Marker tıklandığında konumu göster
            marker.position?.let {
                onMarkerClick(it)
            }
            true
        }

        // Konum alındığında, haritayı o noktaya zoom yapacak şekilde ayarla
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val currentLocation = LatLng(it.latitude, it.longitude)

                // Haritayı o noktaya zoom yapacak şekilde ayarlıyoruz
                val cameraPosition = CameraPosition.Builder()
                    .target(currentLocation)  // Kameranın odaklanacağı nokta
                    .zoom(16f)  // Zoom seviyesi
                    .build()

                googleMap?.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

                // Konum üzerine marker ekleyelim
                googleMap?.addMarker(MarkerOptions().position(currentLocation).title("Buradayım!"))
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // İzin verildi, konum takibini başlat
                startLocationUpdates()
            } else {
                // İzin reddedildi, kullanıcıya açıklama yapabilirsiniz
                Toast.makeText(this, "Konum izni gerekli", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // MapView Yaşam Döngüsü Metotları
    override fun onResume() {
        super.onResume()
        mapView?.onResume()
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
