package com.example.gpstrackingapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.gpstrackingapp.utils.MapUtils

class ActivityViewModel : ViewModel() {

    private val _markersAdded = MutableLiveData<Boolean>()
    val markersAdded: LiveData<Boolean> get() = _markersAdded

    /**
     * Haritaya 100 metrede bir marker ekler.
     *
     * @param googleMap Harita nesnesi.
     * @param startLatLng Başlangıç noktasının koordinatları.
     * @param totalMarkers Eklenmesi gereken toplam marker sayısı.
     */
    fun addMarkersAtIntervals(googleMap: GoogleMap, startLatLng: LatLng, totalMarkers: Int = 10) {
        // Marker eklemek için MapUtils'daki fonksiyonu kullanır
        MapUtils.addMarkersAtIntervals(googleMap, startLatLng, totalMarkers = totalMarkers)

        // İşlem tamamlandığında LiveData'yı günceller
        _markersAdded.value = true
    }
}
