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
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.tracker.expensetracker.ExpenseTrackerApplication
import com.tracker.expensetracker.MainActivity
import com.tracker.expensetracker.domain.model.BudgetStatus
import com.tracker.expensetracker.domain.model.calculateBudgetProgress
import kotlinx.coroutines.flow.first
import java.time.YearMonth

class BudgetAlertWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!canPost(applicationContext)) return Result.success()
        val app = applicationContext as ExpenseTrackerApplication
        val month = YearMonth.now()
        val overBudget = try {
            val limits = app.container.budgetRepository.observeLimits().first()
            if (limits.isEmpty()) return Result.success()
            val expenses = app.container.expenseRepository
                .getExpensesByDateRange(month.atDay(1), month.atEndOfMonth()).first()
            calculateBudgetProgress(month, limits, expenses).filter { it.status == BudgetStatus.OVER_BUDGET }
        } catch (_: SQLiteException) {
            return if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
        if (overBudget.isEmpty()) return Result.success()

        val manager = app.getSystemService(NotificationManager::class.java) ?: return Result.success()
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Budget alerts", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val alerts = app.getSharedPreferences("budget_alerts", Context.MODE_PRIVATE)
        val monthKey = month.toString()
        val openApp = PendingIntent.getActivity(
            app,
            0,
            Intent(app, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        for (budget in overBudget) {
            val key = "posted/${budget.limit.categoryId.orEmpty()}"
            if (alerts.getString(key, null) == monthKey) continue
            if (!canPost(app)) break

            val title = if (budget.limit.categoryId == null) "Monthly budget exceeded"
                else "Category budget exceeded"
            val notification = NotificationCompat.Builder(app, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText("Open Expense Tracker to review your budgets.")
                .setContentIntent(openApp)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .build()
            try {
                // The tag also prevents multiple copies if a worker is interrupted after posting.
                manager.notify("$key/$monthKey", NOTIFICATION_ID, notification)
                alerts.edit().putString(key, monthKey).apply()
            } catch (_: SecurityException) {
                // Permission can be revoked between the check and posting.
                break
            }
        }
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "budget_alerts"
        private const val NOTIFICATION_ID = 1

        fun canPost(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) return false

            val manager = context.getSystemService(NotificationManager::class.java) ?: return false
            return manager.areNotificationsEnabled() &&
                manager.getNotificationChannel(CHANNEL_ID)?.importance != NotificationManager.IMPORTANCE_NONE
        }

        fun schedule(context: Context) {
            if (!canPost(context)) return
            WorkManager.getInstance(context).enqueueUniqueWork(
                "budget_alerts",
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                OneTimeWorkRequestBuilder<BudgetAlertWorker>().build()
            )
        }
    }
}
