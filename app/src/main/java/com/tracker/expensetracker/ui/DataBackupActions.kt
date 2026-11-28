package com.tracker.expensetracker.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tracker.expensetracker.R
import java.time.LocalDate

private enum class BackupDialog { OPTIONS, CREATE, SHARE, RESTORE }

@Composable
fun DataBackupActions(viewModel: DataBackupViewModel) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var dialog by rememberSaveable { mutableStateOf<BackupDialog?>(null) }
    var restoreSource by rememberSaveable { mutableStateOf<String?>(null) }

    val saveCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
        viewModel::saveCsv,
    )
    val saveBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
        viewModel::onBackupDestination,
    )
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            restoreSource = uri.toString()
            dialog = BackupDialog.RESTORE
        }
    }
    fun launchPicker(action: () -> Unit) {
        try {
            action()
        } catch (_: ActivityNotFoundException) {
            viewModel.onBackupDestination(null)
            viewModel.showMessage(R.string.backup_picker_unavailable)
        }
    }

    LaunchedEffect(state.shareDocument) {
        val document = state.shareDocument ?: return@LaunchedEffect
        viewModel.clearShareDocument()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = document.mimeType
            putExtra(Intent.EXTRA_STREAM, document.uri)
            clipData = ClipData.newRawUri(context.getString(R.string.backup_title), document.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(Intent.createChooser(intent, null))
        } catch (_: ActivityNotFoundException) {
            viewModel.showMessage(R.string.backup_share_unavailable)
        }
    }

    IconButton(onClick = { dialog = BackupDialog.OPTIONS }) {
        Icon(
            Icons.Default.SaveAlt,
            contentDescription = stringResource(R.string.backup_title),
        )
    }

    when (dialog) {
        BackupDialog.OPTIONS -> AlertDialog(
            onDismissRequest = { if (!state.busy) dialog = null },
            title = { Text(stringResource(R.string.backup_title)) },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(stringResource(R.string.backup_csv_description))
                    TextButton(enabled = !state.busy, onClick = {
                        viewModel.clearMessage()
                        launchPicker { saveCsv.launch("expenses-${LocalDate.now()}.csv") }
                    }) { Text(stringResource(R.string.backup_save_csv)) }
                    TextButton(enabled = !state.busy, onClick = viewModel::shareCsv) {
                        Text(stringResource(R.string.backup_share_csv))
                    }
                    TextButton(enabled = !state.busy, onClick = {
                        viewModel.clearMessage()
                        dialog = BackupDialog.CREATE
                    }) { Text(stringResource(R.string.backup_create)) }
                    TextButton(enabled = !state.busy, onClick = {
                        viewModel.clearMessage()
                        dialog = BackupDialog.SHARE
                    }) { Text(stringResource(R.string.backup_share_encrypted)) }
                    TextButton(enabled = !state.busy, onClick = {
                        viewModel.clearMessage()
                        launchPicker { openBackup.launch(arrayOf("*/*")) }
                    }) { Text(stringResource(R.string.backup_restore)) }

                    if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    state.message?.let { Text(stringResource(it)) }
                    state.restored?.let {
                        Text(stringResource(R.string.backup_restored, it.expenses, it.categories, it.budgets))
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = !state.busy, onClick = { dialog = null }) {
                    Text(stringResource(R.string.backup_close))
                }
            },
        )
        BackupDialog.CREATE -> BackupPassphraseDialog(
            creating = true,
            onCancel = { dialog = BackupDialog.OPTIONS },
            onConfirm = { passphrase ->
                viewModel.prepareBackup(passphrase)
                dialog = BackupDialog.OPTIONS
                launchPicker { saveBackup.launch("expense-tracker-backup-${LocalDate.now()}.json") }
            },
        )
        BackupDialog.SHARE -> BackupPassphraseDialog(
            creating = true,
            sharing = true,
            onCancel = { dialog = BackupDialog.OPTIONS },
            onConfirm = { passphrase ->
                viewModel.shareBackup(passphrase)
                dialog = BackupDialog.OPTIONS
            },
        )
        BackupDialog.RESTORE -> BackupPassphraseDialog(
            creating = false,
            onCancel = {
                restoreSource = null
                dialog = BackupDialog.OPTIONS
            },
            onConfirm = { passphrase ->
                restoreSource?.let { viewModel.restore(Uri.parse(it), passphrase) }
                restoreSource = null
                dialog = BackupDialog.OPTIONS
            },
        )
        null -> Unit
    }
}

@Composable
private fun BackupPassphraseDialog(
    creating: Boolean,
    sharing: Boolean = false,
    onCancel: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var passphrase by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    val minimumLength = 12
    val ready = if (creating) passphrase.length >= minimumLength && passphrase.isNotBlank() &&
        passphrase == confirmation else passphrase.isNotEmpty()

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(stringResource(when {
                sharing -> R.string.backup_share_encrypted
                creating -> R.string.backup_create
                else -> R.string.backup_restore
            }))
        },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    if (creating) stringResource(R.string.backup_create_help, minimumLength)
                    else stringResource(R.string.backup_restore_help)
                )
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text(stringResource(R.string.backup_passphrase)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (creating) {
                    OutlinedTextField(
                        value = confirmation,
                        onValueChange = { confirmation = it },
                        label = { Text(stringResource(R.string.backup_confirm_passphrase)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = ready, onClick = { onConfirm(passphrase) }) {
                Text(stringResource(when {
                    sharing -> R.string.backup_share_action
                    creating -> R.string.backup_choose_location
                    else -> R.string.backup_restore_action
                }))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}
