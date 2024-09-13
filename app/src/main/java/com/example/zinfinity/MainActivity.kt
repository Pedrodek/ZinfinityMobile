package com.example.zinfinity

import android.app.ActivityManager
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.BatteryManager
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.os.Build
import android.os.PowerManager
import android.provider.DocumentsContract.Root
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.example.zinfinity.background.OptimizationService
import com.example.zinfinity.background.scheduleOptimizationJob
import com.example.zinfinity.hardwareMonitors.CpuMonitor
import com.example.zinfinity.hardwareMonitors.ProcessKiller
import com.example.zinfinity.hardwareMonitors.RamMonitor
import kotlinx.coroutines.*
import kotlin.math.roundToInt
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest



class MainActivity : AppCompatActivity() {

    private lateinit var cpuUsageTextView: TextView
    private lateinit var ramUsageTextView: TextView
    private lateinit var progressCpu: ProgressBar
    private lateinit var progressRam: ProgressBar
    private lateinit var Lgpdbutton: Button
    private lateinit var killButton: Button
    private lateinit var doubleButton: Button
    private lateinit var tvFiles: TextView

    private lateinit var processKiller: ProcessKiller
    private lateinit var cpuMonitor: CpuMonitor
    private lateinit var ramMonitor: RamMonitor

    private val REQUEST_CODE = 100 // Código de solicitação único

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    val rootDirectory = File("/") // Diretório raiz do dispositivo (use "/storage/emulated/0" para armazenamento interno)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        cpuUsageTextView = findViewById(R.id.tvCpuUsage)
        ramUsageTextView = findViewById(R.id.tvRamUsage)
        tvFiles = findViewById(R.id.tvFiles)

        progressCpu = findViewById(R.id.progressCpu)
        progressRam = findViewById(R.id.progressRam)

        Lgpdbutton = findViewById(R.id.Lgpdbutton)
        killButton = findViewById(R.id.KILL)
        doubleButton = findViewById(R.id.button2)

        processKiller = ProcessKiller(this)
        cpuMonitor = CpuMonitor(this)
        ramMonitor = RamMonitor(this)

        // Start the periodic updates using coroutines
        startPeriodicUpdates()

        Lgpdbutton.setOnClickListener {
            val intent = Intent(this, LgpdActivity::class.java)
            startActivity(intent)
        }

        // Set the KILL button click listener
        killButton.setOnClickListener {
            processKiller.killBackgroundProcesses()
            keepCpuAwake(this)
        }
        requestStoragePermission()
        doubleButton.setOnClickListener {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_CODE)
            iterateFilesAndHashInBackground(rootDirectory)
        }

        startForegroundService()
        scheduleOptimizationJob(this)
    }

    private fun startForegroundService() {
        val intent = Intent(this, OptimizationService::class.java)
        startForegroundService(intent)
    }


    private fun startPeriodicUpdates() {
        coroutineScope.launch {
            while (true) {
                updateUI()
                delay(1000) // Delay of 1 second
            }
        }
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



    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.coroutineContext.cancelChildren() // Cancel coroutines to prevent leaks

    }

    override fun onPause() {
        super.onPause()
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
    }

    // Mantem a CPU rodando mesmo com a tela apagada
    var wakeLock: PowerManager.WakeLock? = null
    fun keepCpuAwake(context: Context) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::CPUWakeLock")
        wakeLock?.acquire(1000)
    }





    //BATTERY
//    private fun getBatteryInfo(): Pair<Int, String> {
//        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
//            registerReceiver(null, ifilter)
//        }
//
//        val batteryPct: Int? = batteryStatus?.let { intent ->
//            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
//            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
//            (level / scale.toFloat() * 100).toInt()
//        }
//
//        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
//        val batteryStatusString = when (status) {
//            BatteryManager.BATTERY_STATUS_CHARGING -> "Carregando"
//            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Descarregando"
//            BatteryManager.BATTERY_STATUS_FULL -> "Carregada"
//            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Não Carregando"
//            else -> "Desconhecido"
//        }
//
//        return Pair(batteryPct ?: 0, batteryStatusString)
//    }

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

    //Big Files & Duplicates
    fun getFileHash(file: File): String {
        val digest = MessageDigest.getInstance("MD5")
        val inputStream = file.inputStream()
        val buffer = ByteArray(1024)
        var bytesRead: Int

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }

        inputStream.close()
        val hashBytes = digest.digest()
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun iterateFilesAndHashInBackground(dir: File) {
        coroutineScope.launch(Dispatchers.IO) {
            iterateFilesAndHash(dir)
        }
    }

    fun iterateFilesAndHash(dir: File) {
        // Listar todos os arquivos e diretórios dentro do diretório atual
        val files = dir.listFiles()

        files?.forEach { file ->
            if ((checkSelfPermission("READ_EXTERNAL_STORAGE") == PackageManager.PERMISSION_GRANTED)) { //checar
                if (file.isDirectory) {
                    // Se for um diretório, chamar a função
                    iterateFilesAndHash(file)
                } else {
                    // Se for um arquivo, calcular o hash
                    val fileHash = getFileHash(file)
                    println("Arquivo: ${file.absolutePath}, Hash: $fileHash")
                }
            }
        }
    }

    private fun requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_CODE
            )
        } else {
            // Permissão já concedida, você pode continuar com a funcionalidade que requer a permissão
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissão concedida, continue com a funcionalidade que requer a permissão
            } else {
                // Permissão negada, informe o usuário ou desative a funcionalidade
            }
        }
    }
}





