package com.tracker.expensetracker.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteException
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.tracker.expensetracker.ExpenseTrackerApplication
import com.tracker.expensetracker.MainActivity
import com.tracker.expensetracker.R
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class RecurringExpenseWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as ExpenseTrackerApplication
        val today = LocalDate.now()
        val posted = try {
            app.container.recurringExpenseRepository.postDueExpenses(today)
        } catch (_: SQLiteException) {
            return if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
        if (posted > 0) {
            postConfirmation(app, today, posted)
            BudgetAlertWorker.schedule(app)
        }
        return Result.success()
    }

    private fun postConfirmation(context: Context, today: LocalDate, posted: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (!manager.areNotificationsEnabled()) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL,
                context.getString(R.string.recurring_notifications_title),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
        )
        val intent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.recurring_posted_title))
            .setContentText(context.resources.getQuantityString(R.plurals.recurring_posted, posted, posted))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(intent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        try {
            manager.notify(today.toString(), NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // The notification permission can change after the check.
        }
    }

    companion object {
        private const val CHANNEL = "recurring_expenses"
        private const val NOTIFICATION_ID = 4

        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "recurring_expenses_daily",
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<RecurringExpenseWorker>(1, TimeUnit.DAYS).build(),
            )
            enqueueCheck(context, ExistingWorkPolicy.KEEP)
        }

        fun afterScheduleChange(context: Context) = enqueueCheck(context, ExistingWorkPolicy.APPEND_OR_REPLACE)

        private fun enqueueCheck(context: Context, policy: ExistingWorkPolicy) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                "recurring_expenses_check",
                policy,
                OneTimeWorkRequestBuilder<RecurringExpenseWorker>().build(),
            )
        }
    }
}
