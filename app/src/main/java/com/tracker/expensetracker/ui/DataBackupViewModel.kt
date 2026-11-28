package com.tracker.expensetracker.ui

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tracker.expensetracker.ExpenseTrackerApplication
import com.tracker.expensetracker.R
import com.tracker.expensetracker.data.backup.BackupTooLargeException
import com.tracker.expensetracker.data.backup.DataBackupRepository
import com.tracker.expensetracker.data.backup.InvalidBackupException
import com.tracker.expensetracker.data.backup.UnsupportedBackupVersionException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.GeneralSecurityException

data class DataBackupUiState(
    val busy: Boolean = false,
    @param:StringRes val message: Int? = null,
    val restored: DataBackupRepository.RestoreResult? = null,
    val shareUri: Uri? = null,
)

class DataBackupViewModel(private val repository: DataBackupRepository) : ViewModel() {
    private val _state = MutableStateFlow(DataBackupUiState())
    val state = _state.asStateFlow()

    // Keep the passphrase across rotation while the picker is open, never in saved state.
    private var pendingPassphrase: CharArray? = null

    fun prepareBackup(passphrase: String) {
        pendingPassphrase?.fill('\u0000')
        pendingPassphrase = passphrase.toCharArray()
    }

    fun onBackupDestination(destination: Uri?) {
        val passphrase = pendingPassphrase
        pendingPassphrase = null
        when {
            destination == null -> passphrase?.fill('\u0000')
            passphrase == null -> showMessage(R.string.backup_passphrase_required_again)
            else -> perform(passphrase) {
                repository.saveBackup(destination, passphrase)
                DataBackupUiState(message = R.string.backup_saved)
            }
        }
    }

    fun saveCsv(destination: Uri?) {
        if (destination == null) return
        perform {
            repository.saveCsv(destination)
            DataBackupUiState(message = R.string.backup_csv_saved)
        }
    }

    fun shareCsv() = perform {
        DataBackupUiState(shareUri = repository.createShareableCsv())
    }

    fun restore(source: Uri, passphrase: String) {
        val password = passphrase.toCharArray()
        perform(password) {
            DataBackupUiState(restored = repository.restoreBackup(source, password))
        }
    }

    fun showMessage(@StringRes message: Int) {
        _state.value = DataBackupUiState(message = message)
    }

    fun clearMessage() {
        _state.update { it.copy(message = null, restored = null) }
    }

    fun clearShareUri() {
        _state.update { it.copy(shareUri = null) }
    }

    override fun onCleared() {
        pendingPassphrase?.fill('\u0000')
        pendingPassphrase = null
    }

    private fun perform(
        passphrase: CharArray? = null,
        work: suspend () -> DataBackupUiState,
    ) {
        if (_state.value.busy) {
            passphrase?.fill('\u0000')
            return
        }
        _state.value = DataBackupUiState(busy = true)
        viewModelScope.launch {
            try {
                _state.value = work()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                val message = when (error) {
                    is GeneralSecurityException -> R.string.backup_authentication_failed
                    is InvalidBackupException -> R.string.backup_invalid
                    is UnsupportedBackupVersionException -> R.string.backup_unsupported_version
                    is BackupTooLargeException -> R.string.backup_too_large
                    else -> R.string.backup_io_failed
                }
                showMessage(message)
            } finally {
                passphrase?.fill('\u0000')
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ExpenseTrackerApplication
                DataBackupViewModel(app.container.dataBackupRepository)
            }
        }
    }
}
