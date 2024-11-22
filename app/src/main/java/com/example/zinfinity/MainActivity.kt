package com.example.zinfinity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.zinfinity.background.OptimizationService
import com.example.zinfinity.hardwareMonitors.ProcessKiller
import kotlinx.coroutines.*
import java.io.File
import java.io.FileNotFoundException
import java.security.MessageDigest


class MainActivity : AppCompatActivity() {

    private lateinit var Lgpdbutton: Button
    private lateinit var Intelbutton: Button
    private lateinit var killButton: Button
    private lateinit var doubleButton: Button
    //private lateinit var tvFiles: TextView
    private lateinit var processKiller: ProcessKiller
    private var isServiceRunning = false


    private val REQUEST_CODE = 100 // Código de solicitação único

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    val rootDirectory = File("/") // Diretório raiz do dispositivo (use "/storage/emulated/0" para armazenamento interno)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = Color.TRANSPARENT
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }

        //tvFiles = findViewById(R.id.tvFiles)

        Lgpdbutton = findViewById(R.id.Lgpdbutton)
        Intelbutton = findViewById(R.id.Intelbutton)

        killButton = findViewById(R.id.KILL)
        doubleButton = findViewById(R.id.button2)

        processKiller = ProcessKiller(this)

        Lgpdbutton.setOnClickListener {
            val intent = Intent(this, LgpdActivity::class.java)
            startActivity(intent)
        }

        Intelbutton.setOnClickListener {
            val intent = Intent(this, IntelActivity::class.java)
            startActivity(intent)
        }


        killButton.text = "Iniciar"

        killButton.setOnClickListener {
            if (isServiceRunning) {
                stopOptimizationService()
                killButton.text = "Iniciar"
            } else {
                startOptimizationService()
                killButton.text = "Parar"
            }
            isServiceRunning = !isServiceRunning
        }

        requestStoragePermission()
        doubleButton.setOnClickListener {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_CODE)
            iterateFilesAndHashInBackground(rootDirectory)
        }

    }

    private fun startOptimizationService() {
        val intent = Intent(this, OptimizationService::class.java)
        intent.action = "START"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopOptimizationService() {
        val intent = Intent(this, OptimizationService::class.java)
        intent.action = "STOP"
        stopService(intent)
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



    //Big Files & Duplicates
    fun getFileHash(file: File): String {
        val digest = MessageDigest.getInstance("MD5")
        file.inputStream().use { inputStream ->
            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        val hashBytes = digest.digest()
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun iterateFilesAndHashInBackground(dir: File) {
        coroutineScope.launch(Dispatchers.IO) {
            iterateFilesAndHash(dir)
            delay(200)
        }
    }
//------------------------------------------------------------------------------------------------------------------
//  Anotações de arquivos que podem ser interessantes

    // /system/app/GameOptimizer/GameOptimizer.apk

    // Arquivo: /system/priv-app/GoogleFeedback/GoogleFeedback.apk, Hash: b87b96253ed6e743cc54bd759e55c7a5, Tamanho: 490,20 KB
    // 2024-09-13 09:13:07.944 17458-17649 System.out              com.example.zinfinity                I  Arquivo: /system/priv-app/GoogleFeedback/oat/arm64/GoogleFeedback.odex, Hash: 8c73ed46df16d326740b4c7d6004b8fe, Tamanho: 80,70 KB
    // 2024-09-13 09:13:07.960 17458-17649 System.out              com.example.zinfinity                I  Arquivo: /system/priv-app/GoogleFeedback/oat/arm64/GoogleFeedback.vdex, Hash: b5fed3f5668713cc37c23b1a7d89602d, Tamanho: 493,18 KB

//------------------------------------------------------------------------------------------------------------------

    fun iterateFilesAndHash(dir: File) {
        // Listar todos os arquivos e diretórios dentro do diretório atual
        val files = dir.listFiles()

        files?.forEach { file ->
            try {
                // Checar se temos permissão para ler o arquivo ou diretório
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {

                    if (file.isDirectory) {
                        // Se for um diretório, chamar a função recursivamente
                        iterateFilesAndHash(file)
                    } else {
                        // Se for um arquivo, calcular o hash e obter o tamanho
                        val fileHash = getFileHash(file)
                        val fileSize = getFileSize(file.length())
                        println("Arquivo: ${file.absolutePath}, Hash: $fileHash, Tamanho: $fileSize")
                    }
                } else {
                    println("Permissão negada para acessar ${file.absolutePath}")
                }
            } catch (e: SecurityException) {
                // Ignorar diretórios ou arquivos que não podem ser acessados por falta de permissão
                println("Acesso negado a: ${file.absolutePath}, erro de permissão: ${e.message}")
            } catch (e: FileNotFoundException) {
                // Ignorar arquivos ou diretórios não encontrados
                println("Arquivo ou diretório não encontrado: ${file.absolutePath}, erro: ${e.message}")
            } catch (e: Exception) {
                // Capturar outras exceções para evitar que o código quebre
                println("Erro ao acessar ${file.absolutePath}: ${e.message}")
            }
        }
    }

    // Função para converter o tamanho do arquivo para KB, MB ou GB
    fun getFileSize(sizeInBytes: Long): String {
        val sizeInKb = sizeInBytes / 1024.0
        val sizeInMb = sizeInKb / 1024.0
        val sizeInGb = sizeInMb / 1024.0

        return when {
            sizeInGb >= 1 -> String.format("%.2f GB", sizeInGb)
            sizeInMb >= 1 -> String.format("%.2f MB", sizeInMb)
            else -> String.format("%.2f KB", sizeInKb)
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





