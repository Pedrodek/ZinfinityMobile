package com.example.zinfinity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    val rootDirectory = File("/") // Diretório raiz do dispositivo (use "/storage/emulated/0" para armazenamento interno)

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
                map.text = "Mapear"
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_CODE)
                iterateFilesAndHashInBackground(rootDirectory)
            } else {
                isActive = false
                map.text = "Interromper mapeamento"
                backgroundJob?.cancel() // Isso ainda n funciona
            }
        }

        logTextView = findViewById(R.id.logTextView)



        val navigationView: NavigationView = findViewById(R.id.navigation_view)
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.Extra -> {
                    Toast.makeText(this, "Home clicked", Toast.LENGTH_SHORT).show()

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
        backgroundJob = coroutineScope.launch(Dispatchers.IO) {
            try {
                iterateFilesAndHash(dir)
                delay(200)
            } catch (e: CancellationException) {
                println("Processo foi interrompido.")
            }
        }
    }

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
}
