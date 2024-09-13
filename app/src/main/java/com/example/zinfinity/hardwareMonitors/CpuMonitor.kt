package com.example.zinfinity.hardwareMonitors

import android.app.ActivityManager
import android.content.Context

class CpuMonitor(private val context: Context) {
    //CPU
    fun getAppCpuUsage(): Int {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningAppProcesses = activityManager.runningAppProcesses ?: return 0
        // Total RAM MB
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val totalRamMB = memoryInfo.totalMem / 1048576L // Convertendo de bytes para MB
        // Calcular o uso médio de memória em MB
        var totalCpuUsageKB = 0
        for (process in runningAppProcesses) {
            val pid = process.pid
            val pInfo = activityManager.getProcessMemoryInfo(intArrayOf(pid))
            val memoryUsageKB = pInfo[0].totalPrivateDirty // Usando memória como proxy em KB
            totalCpuUsageKB += memoryUsageKB
        }
        val averageCpuUsageMB = totalCpuUsageKB / 1024 / runningAppProcesses.size // Média em MB
        // Calcular o percentual de uso da CPU
        val cpuUsagePercentage = (averageCpuUsageMB.toDouble() / totalRamMB.toDouble()) * 100
        return cpuUsagePercentage.toInt()
    }
}