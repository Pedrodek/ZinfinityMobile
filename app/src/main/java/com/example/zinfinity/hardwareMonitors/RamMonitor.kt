package com.example.zinfinity.hardwareMonitors

import android.app.ActivityManager
import android.content.Context
import kotlin.math.roundToInt

class RamMonitor(private val context: Context) {
    //RAM
    fun getRamUsage(): Pair<Long, Int> {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val totalMegs = memoryInfo.totalMem / 1048576L
        val availableMegs = memoryInfo.availMem / 1048576L
        val percentAvail = (memoryInfo.availMem / memoryInfo.totalMem.toDouble() * 100.0).roundToInt()

        return Pair(availableMegs, percentAvail)
    }
}