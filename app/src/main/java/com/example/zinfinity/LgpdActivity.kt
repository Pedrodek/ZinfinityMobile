package com.example.zinfinity

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LgpdActivity : AppCompatActivity() {

    private lateinit var tvLgpd: TextView
    private lateinit var backBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lgpd)

        tvLgpd = findViewById(R.id.tvLgpd)
        backBtn = findViewById(R.id.back)

        // Define o texto do TextView como vazio
        tvLgpd.text = "Este App pode coletar as seguintes informações do seu dispositivo:\n" +
                "- Cache dos aplicativos\n" +
                "- Informações de hardware (CPU, RAM e etc)\n" +
                "- Informações sobre o software do celular\n" +
                "Tenha em mente que o aplicativo coleta e usa esses dados para melhorar a performance do seu dispositivo e fazer com que ele funcione de forma mais eficiente, esses dados nunca são enviados ou armazenados fora do seu dispositivo.\n"

        // Implementa o clique no botão "Voltar"
        backBtn.setOnClickListener {
            finish()  // Fecha a Activity e volta para a anterior
        }
    }
}