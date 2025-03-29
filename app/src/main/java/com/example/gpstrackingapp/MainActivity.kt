package com.example.gpstrackingapp  // Proje package name'ini kendi projenle değiştir

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(null) // Eğer layout kullanmayacaksan null bırak
    }
}
