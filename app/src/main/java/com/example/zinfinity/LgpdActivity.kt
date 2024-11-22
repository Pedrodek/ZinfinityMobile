package com.example.zinfinity

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LgpdActivity : AppCompatActivity() {

    private lateinit var tvLgpd: TextView
    private lateinit var backBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lgpd)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = Color.TRANSPARENT
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }

        tvLgpd = findViewById(R.id.tvLgpd)
        backBtn = findViewById(R.id.back)

        // Implementa o clique no botão "Voltar"
        backBtn.setOnClickListener {
            finish()  // Fecha a Activity e volta para a anterior
        }
    }
}