package com.example.zinfinity.hardwareMonitors

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProcessKiller(private val context: Context) {

    // KILL Bkg
    fun killBackgroundProcesses() {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val packageManager = context.packageManager
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        CoroutineScope(Dispatchers.IO).launch {
            for (packageInfo in packages) {
                activityManager.killBackgroundProcesses(packageInfo.packageName)
            }
        }
    }
}
