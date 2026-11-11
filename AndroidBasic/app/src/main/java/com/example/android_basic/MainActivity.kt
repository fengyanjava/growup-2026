package com.example.android_basic

import android.os.Bundle
import android.content.Intent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.android_basic.core.ui.applySystemBarInsets
import com.example.android_basic.databinding.ActivityMainBinding
import com.example.android_basic.feature.mvvm.ui.PostListActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        binding.openMvvmButton.setOnClickListener {
            startActivity(Intent(this, PostListActivity::class.java))
        }
    }
}
