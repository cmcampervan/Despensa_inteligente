package com.example.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkScheduler {
    private const val WORK_NAME = "PantryExpirationCheckWork"

    fun scheduleExpirationCheckWork(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<PantryExpirationWorker>(
            24, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
    }

    fun triggerImmediateCheck(context: Context) {
        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<PantryExpirationWorker>().build()
        WorkManager.getInstance(context).enqueue(oneTimeWorkRequest)
    }
}

