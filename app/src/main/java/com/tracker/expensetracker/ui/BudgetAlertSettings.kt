package com.tracker.expensetracker.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.tracker.expensetracker.notifications.BudgetAlertWorker

private enum class BudgetPermissionPrompt { RATIONALE, SETTINGS }

@Composable
fun BudgetAlertSettings() {
    val activity = LocalActivity.current ?: return
    val permission = Manifest.permission.POST_NOTIFICATIONS
    val requests = activity.getSharedPreferences("budget_alerts", Context.MODE_PRIVATE)
    var prompt by rememberSaveable { mutableStateOf<BudgetPermissionPrompt?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            BudgetAlertWorker.schedule(activity)
        } else if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
            prompt = BudgetPermissionPrompt.SETTINGS
        }
    }

    val requestPermission = {
        requests.edit().putBoolean("permission_requested", true).apply()
        launcher.launch(permission)
    }

    TextButton(onClick = {
        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED -> {
                activity.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
                )
            }
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission) -> {
                prompt = BudgetPermissionPrompt.RATIONALE
            }
            requests.getBoolean("permission_requested", false) -> {
                prompt = BudgetPermissionPrompt.SETTINGS
            }
            else -> requestPermission()
        }
    }) {
        Text(if (BudgetAlertWorker.canPost(activity)) "Alert settings" else "Enable budget alerts")
    }

    prompt?.let { activePrompt ->
        val requestsAccess = activePrompt == BudgetPermissionPrompt.RATIONALE
        AlertDialog(
            onDismissRequest = { prompt = null },
            title = { Text("Budget alerts") },
            text = { Text("Allow notifications to get an alert when spending exceeds a budget.") },
            confirmButton = {
                TextButton(onClick = {
                    prompt = null
                    if (requestsAccess) {
                        requestPermission()
                    } else {
                        activity.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", activity.packageName, null)
                            )
                        )
                    }
                }) { Text(if (requestsAccess) "Continue" else "Open settings") }
            },
            dismissButton = { TextButton(onClick = { prompt = null }) { Text("Cancel") } }
        )
    }
}
