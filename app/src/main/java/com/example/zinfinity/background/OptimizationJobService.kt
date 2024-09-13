package com.example.zinfinity.background

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import com.example.zinfinity.hardwareMonitors.CpuMonitor
import com.example.zinfinity.hardwareMonitors.ProcessKiller
import com.example.zinfinity.hardwareMonitors.RamMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun scheduleOptimizationJob(context: Context) {
    val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
    val componentName = ComponentName(context, OptimizationJobService::class.java)

    val jobInfo = JobInfo.Builder(123, componentName)
        .setRequiresCharging(false)
        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        .setPersisted(true)
        .setPeriodic(15 * 60 * 1000) // Executa a cada 15 minutos
        .build()

    jobScheduler.schedule(jobInfo)
}

class OptimizationJobService : JobService() {

    private lateinit var processKiller: ProcessKiller

    override fun onStartJob(params: JobParameters?): Boolean {
        // Executa as otimizações aqui
        CoroutineScope(Dispatchers.IO).launch {
            optimizeDevicePerformance()
            jobFinished(params, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        return true
    }

    private suspend fun optimizeDevicePerformance() {
        //CPU

        val cpuUsage = CpuMonitor(this).getAppCpuUsage()
        val (availableMegs, percentAvail) = RamMonitor(this).getRamUsage()

        // Kill BKG
        processKiller = ProcessKiller(this)
        if (cpuUsage > 80 || (100 - percentAvail) > 85) {
            processKiller.killBackgroundProcesses()
        }
    }
}
