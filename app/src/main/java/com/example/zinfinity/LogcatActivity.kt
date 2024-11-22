package com.example.zinfinity

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class LogcatActivity : AppCompatActivity() {

    private lateinit var logTextView: TextView
    private lateinit var logFile: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_logcat)
        logFile = File(filesDir, "logcat_temp.txt")

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        logTextView = findViewById(R.id.logTextView)

        // Função para capturar o Logcat em tempo real
        fun captureLogcatInRealTime(onNewLog: (String) -> Unit) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val process = Runtime.getRuntime().exec("logcat")
                    val bufferedReader = process.inputStream.bufferedReader()

                    var line: String?
                    while (bufferedReader.readLine().also { line = it } != null) {
                        withContext(Dispatchers.Main) {
                            onNewLog(line ?: "")
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }




        captureLogcatInRealTime { newLog ->
            logFile.appendText("$newLog\n")

            runOnUiThread {
                val logs = logFile.readLines().takeLast(200).joinToString("\n")
                logTextView.text = logs

                // Rolar automaticamente para o final
                logTextView.post {
                    val layout = logTextView.layout
                    if (layout != null) {
                        val scrollAmount = layout.getLineTop(logTextView.lineCount) - logTextView.height
                        if (scrollAmount > 0) {
                            logTextView.scrollTo(0, scrollAmount)
                        }
                    }
                }
            }
        }

    }
}
