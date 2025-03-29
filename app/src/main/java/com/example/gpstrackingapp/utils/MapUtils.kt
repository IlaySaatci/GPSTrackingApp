package com.example.gpstrackingapp.utils

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

object MapUtils {

    /**
     * Belirtilen başlangıç noktasından her 100 metrede bir marker ekler.
     *
     * @param googleMap Harita nesnesi.
     * @param startLatLng Başlangıç noktası (latitude, longitude).
     * @param distanceMeasure 100 metrelik mesafeye göre her marker eklenir.
     * @param totalMarkers Eklenmesi gereken toplam marker sayısı.
     */
    fun addMarkersAtIntervals(
        googleMap: GoogleMap,
        startLatLng: LatLng,
        distanceMeasure: Float = 100f, // Varsayılan 100 metre
        totalMarkers: Int = 10 // Varsayılan olarak 10 marker ekle
    ) {
        var currentLocation = startLatLng

        // Başlangıç noktasına marker ekle
        googleMap.addMarker(MarkerOptions().position(currentLocation).title("Başlangıç Noktası"))

        // Her 100 metrede bir marker ekle
        for (i in 1 until totalMarkers) {
            // Koordinatları ilerlet
            currentLocation = getNextLocation(currentLocation, distanceMeasure)
            googleMap.addMarker(MarkerOptions().position(currentLocation).title("Marker $i"))
        }
    }

    /**
     * Verilen bir noktadan belirtilen mesafeye göre yeni bir koordinat hesaplar.
     *
     * @param currentLatLng Şu anki koordinat.
     * @param distanceMetre Mesafe.
     * @return Yeni LatLng koordinat.
     */
    fun getNextLocation(currentLatLng: LatLng, distanceMetre: Float): LatLng {
        val radius = 6371000  // Dünya yarıçapı (metre cinsinden)
        val deltaLat = distanceMetre / radius  // Derece cinsinden

        val newLat = currentLatLng.latitude + Math.toDegrees(deltaLat.toDouble())
        val newLng = currentLatLng.longitude

        return LatLng(newLat, newLng)
    }
}
