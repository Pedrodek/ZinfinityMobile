package com.example.zinfinity.background

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.example.zinfinity.R
import com.example.zinfinity.hardwareMonitors.CpuMonitor
import com.example.zinfinity.hardwareMonitors.ProcessKiller
import com.example.zinfinity.hardwareMonitors.RamMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class OptimizationService : Service() {

    private lateinit var processKiller: ProcessKiller
//    private lateinit var cpuMonitor: CpuMonitor
//    private lateinit var ramMonitor: RamMonitor

    override fun onCreate() {
        super.onCreate()
        val notificationChannel = NotificationChannel(
            "OptimizationService",
            "Optimization Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(notificationChannel)

        val notification = Notification.Builder(this, "OptimizationService")
            .setContentTitle("Mantendo uma boa performance")
            .setContentText("Executando otimizações no dispositivo em segundo plano.")
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .build()

        startForeground(1, notification)
        // Iniciar otimizações em segundo plano
        startOptimization()
    }

    private fun startOptimization() {
        // Mover a lógica de otimização (CPU, RAM, arquivos) para cá
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                // Verificar CPU e RAM periodicamente
                optimizeDevicePerformance()
                delay(5000)
            }
        }
    }

    private suspend fun optimizeDevicePerformance() {
        //CPU    (preciso de correções e adaptações)

        val cpuUsage = CpuMonitor(this).getAppCpuUsage()
        val (availableMegs, percentAvail) = RamMonitor(this).getRamUsage()

        // Lógica para otimizar e matar processos, se necessário (Valores baixos nas condições causam um massacre de processos, mas o dispositivo roda muiito bem)
//      // Pode acabar matando processos que não precisava, tipo desinstalar um app
        processKiller = ProcessKiller(this)
        if (cpuUsage > 30 || (100 - percentAvail) > 35) {
            processKiller.killBackgroundProcesses()
        }
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

//    override fun onDestroy() {
//        super.onDestroy()
//        // Liberar recursos e encerrar tarefas quando o serviço for destruído
//    }
}
