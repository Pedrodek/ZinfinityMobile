package com.example.zinfinity

import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.zinfinity.hardwareMonitors.CpuMonitor
import com.example.zinfinity.hardwareMonitors.RamMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IntelActivity : AppCompatActivity() {
    private lateinit var cpuUsageTextView: TextView
    private lateinit var ramUsageTextView: TextView
    private lateinit var progressCpu: ProgressBar
    private lateinit var progressRam: ProgressBar
    private lateinit var cpuMonitor: CpuMonitor
    private lateinit var ramMonitor: RamMonitor

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_intel)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = Color.TRANSPARENT
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }

        cpuUsageTextView = findViewById(R.id.tvCpuUsage)
        ramUsageTextView = findViewById(R.id.tvRamUsage)

        progressCpu = findViewById(R.id.progressCpu)
        progressRam = findViewById(R.id.progressRam)

        cpuMonitor = CpuMonitor(this)
        ramMonitor = RamMonitor(this)

        startPeriodicUpdates()
    }

    private suspend fun updateUI() = withContext(Dispatchers.IO) {
        val cpuUsage = cpuMonitor.getAppCpuUsage() // Pode ser pesado
        val (availableMegs, percentAvail) = ramMonitor.getRamUsage() // Operação de RAM
        val batteryInfo = getBatteryInfo() // Leitura de bateria
        val storageInfo = getStorageInfo() // Leitura de armazenamento

        // Atualiza a UI no dispatcher Main
        withContext(Dispatchers.Main) {
            cpuUsageTextView.text = "CPU Usage: $cpuUsage%"
            ramUsageTextView.text = "RAM Usage: ${100 - percentAvail}%"
            progressRam.progress = 100 - percentAvail

//            findViewById<TextView>(R.id.tvBatteryInfo).text = "Nível da Bateria: ${batteryInfo.first}%\nStatus: ${batteryInfo.second}"
            findViewById<TextView>(R.id.tvBatteryInfo).text = "Status da Bateria: ${batteryInfo}"
            findViewById<TextView>(R.id.tvStorageInfo).text = "Armazenamento Disponível: ${storageInfo.first}MB\nArmazenamento Total: ${storageInfo.second}MB"
        }
    }

    private fun startPeriodicUpdates() {
        coroutineScope.launch {
            while (true) {
                updateUI()
                delay(1000) // Delay of 1 second
            }
        }
    }

    private fun getBatteryInfo(): String {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            registerReceiver(null, ifilter)
        }

        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val batteryStatusString = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Carregando"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Descarregando"
            BatteryManager.BATTERY_STATUS_FULL -> "Carregada"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Não Carregando"
            else -> "Desconhecido"
        }

        return batteryStatusString
    }

    //STORAGE
    private fun getStorageInfo(): Pair<Long, Long> {
        val statFs = StatFs(Environment.getDataDirectory().path)
        val availableBlocks = statFs.availableBlocksLong
        val blockSize = statFs.blockSizeLong
        val totalBlocks = statFs.blockCountLong

        val availableSpace = (availableBlocks * blockSize) / 1048576L // Em MB
        val totalSpace = (totalBlocks * blockSize) / 1048576L // Em MB

        return Pair(availableSpace, totalSpace)
    }
}