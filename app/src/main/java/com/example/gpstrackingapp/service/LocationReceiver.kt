//package com.example.gpstrackingapp.service
//
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import android.util.Log
//import com.google.android.gms.maps.CameraUpdateFactory
//import com.google.android.gms.maps.GoogleMap
//import com.google.android.gms.maps.model.LatLng
//import com.google.android.gms.maps.model.MarkerOptions
//
//class LocationReceiver : BroadcastReceiver() {
//
//    private var googleMap: GoogleMap? = null
//
//    // Harita objesini setter fonksiyonu ile dışarıdan alıyoruz.
//    fun setGoogleMap(map: GoogleMap) {
//        googleMap = map
//    }
//
//    override fun onReceive(context: Context?, intent: Intent?) {
//        Log.e("TAG", "onCreate: help me on receive", )
//
//        val lat = intent?.getDoubleExtra("latitude", 0.0)
//        val lon = intent?.getDoubleExtra("longitude", 0.0)
//
//        Log.d(
//            "LocationReceiver",
//            "Konum alındı: latitude=$lat, longitude=$lon"
//        ) // Bu logu kontrol et
//
//        if (lat != null && lon != null && googleMap != null) {
//            val location = LatLng(lat, lon)
//
//            Log.d(
//                "LocationReceiver",
//                "Marker ekleniyor: $location"
//            ) // Marker ekleniyor diye log da yazdırın
//
//            // Haritaya marker ekliyoruz
//            googleMap?.addMarker(MarkerOptions().position(location).title("Yeni Konum"))
//            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 17f))
//        } else {
//            Log.d("LocationReceiver", "Geçersiz konum: latitude=$lat, longitude=$lon")
//        }
//    }
//}
