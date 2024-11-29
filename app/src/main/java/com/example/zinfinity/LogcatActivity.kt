package com.example.zinfinity

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.security.MessageDigest
import java.util.concurrent.CancellationException

class LogcatActivity : AppCompatActivity() {

    private lateinit var logTextView: TextView
    private lateinit var logFile: File

    private lateinit var burgerbtn: ImageButton
    private lateinit var map: Button

    private var isActive: Boolean = false

    private var backgroundJob: Job? = null

    private lateinit var drawerLayout: DrawerLayout

    private val REQUEST_CODE = 100 // Código de solicitação único

    private var isTextViewPressed = false

    val rootDirectory = File("/") // Diretório raiz do dispositivo (use "/storage/emulated/0" para armazenamento interno)

    @SuppressLint("ClickableViewAccessibility")
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

        drawerLayout = findViewById(R.id.drawer_layout)
        burgerbtn = findViewById(R.id.burgerbtn)
        map = findViewById(R.id.map)

        burgerbtn.setOnClickListener {
            if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        map.setOnClickListener{
            if (!isActive) {
                isActive = true
                checkAndShowPopup()
                map.text = "Interromper mapeamento"
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_CODE)
                iterateFilesAndHashInBackground(rootDirectory)
            } else {
                isActive = false
                stopHashingProcess()
                map.text = "Mapear"
            }
        }

        logTextView = findViewById(R.id.logTextView)



        val navigationView: NavigationView = findViewById(R.id.navigation_view)
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.Inicio -> {
                    finish()
                }
                R.id.Extra -> {
                    val intent = Intent(this, ExtraActivity::class.java)
                    startActivity(intent)

                }
//                R.id.nav_settings -> {
//                    Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show()
//                }
//                R.id.nav_about -> {
//                    Toast.makeText(this, "About clicked", Toast.LENGTH_SHORT).show()
//                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

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
                val logs = logFile.readLines().takeLast(40).joinToString("\n")
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


        val sharedPreferences = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("hasShownPopup", false).apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        logTextView.text = "-"
        if (logFile.exists()) {
            logFile.delete()
        }
    }

    //Big Files & Duplicates
    private fun getFileHash(file: File): String {
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

    private fun iterateFilesAndHashInBackground(dir: File) {
        // Cancela o job anterior, se estiver ativo
        backgroundJob?.cancel()

        // Inicia um novo job
        backgroundJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                iterateFilesAndHash(dir) // Certifique-se de que esta função verifica o cancelamento
                delay(200) // Este delay respeita o cancelamento automaticamente
            } catch (e: CancellationException) {
                println("Processo foi interrompido.") // Log do cancelamento
            } catch (e: Exception) {
                println("Erro inesperado: ${e.message}")
            }
        }
    }

    private fun stopHashingProcess() {
        // Cancela o job
        backgroundJob?.cancel()
        backgroundJob = null
        println("Operação cancelada.")
    }


    private fun iterateFilesAndHash(dir: File) {
        // Listar todos os arquivos e diretórios dentro do diretório atual
        val files = dir.listFiles()

        files?.forEach { file ->
            try {
                // Verifica se a coroutine foi cancelada
                if (!isActive) {
                    println("Processo interrompido antes de acessar ${file.absolutePath}")
                    throw CancellationException()
                }

                // Checar se temos permissão para ler o arquivo ou diretório
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED
                ) {
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
            } catch (e: CancellationException) {
                // Log adicional para indicar cancelamento
                println("Iteração cancelada: ${file.absolutePath}")
                throw e // Repassa o cancelamento para encerrar a coroutine
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

    private fun getFileSize(sizeInBytes: Long): String {
        val sizeInKb = sizeInBytes / 1024.0
        val sizeInMb = sizeInKb / 1024.0
        val sizeInGb = sizeInMb / 1024.0

        return when {
            sizeInGb >= 1 -> String.format("%.2f GB", sizeInGb)
            sizeInMb >= 1 -> String.format("%.2f MB", sizeInMb)
            else -> String.format("%.2f KB", sizeInKb)
        }
    }



    private fun showPopup() {
        AlertDialog.Builder(this)
            .setTitle("Aviso")
            .setMessage("Essa função pode ser um pouco intensiva. Ela listará TODOS os arquivos que ela encontrar e ter acesso. Lembre de desliga-lá! Se não a performance pode ser afetada.")
            .setPositiveButton("Entendi") { dialog, _ -> dialog.dismiss() }
            .setNegativeButton("Cancelar") { dialog, _ -> isActive = false; stopHashingProcess(); map.text = "Mapear" }
            .show()
    }

    private fun checkAndShowPopup() {
        val sharedPreferences = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
        val hasShownPopup = sharedPreferences.getBoolean("hasShownPopup", false)

        if (!hasShownPopup) {
            showPopup()
            sharedPreferences.edit().putBoolean("hasShownPopup", true).apply()
        }
    }
}
