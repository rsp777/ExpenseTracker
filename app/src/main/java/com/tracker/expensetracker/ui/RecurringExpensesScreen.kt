package com.tracker.expensetracker.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tracker.expensetracker.R
import com.tracker.expensetracker.domain.model.Expense
import com.tracker.expensetracker.domain.model.RecurrenceCadence
import com.tracker.expensetracker.domain.model.RecurringExpense
import com.tracker.expensetracker.domain.model.RecurringExpenseDetails
import com.tracker.expensetracker.ui.components.AddEditExpenseSheet
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun RecurringExpensesScreen(viewModel: RecurringExpensesViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val controls = state.interaction
    var pendingCancellation by rememberSaveable { mutableStateOf<Long?>(null) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(enabled = !controls.busy, onClick = { viewModel.openEditor() }) {
                    Text(stringResource(R.string.recurring_add))
                }
                NotificationAccessSettings(
                    label = stringResource(R.string.recurring_notifications_settings),
                    title = stringResource(R.string.recurring_notifications_title),
                    rationale = stringResource(R.string.recurring_notifications_help),
                    onGranted = {},
                )
                if (controls.editor == null) controls.message?.let {
                    Text(stringResource(it), color = MaterialTheme.colorScheme.error)
                }
            }
        }
        if (state.schedules.isEmpty()) {
            item { Text(stringResource(R.string.recurring_empty)) }
        }
        items(state.schedules, key = { it.id }) { schedule ->
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            schedule.details.title,
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(state.currency.format(schedule.details.amount))
                    }
                    Text(
                        stringResource(
                            R.string.recurring_category_and_cadence,
                            categoryName(schedule, state),
                            stringResource(schedule.details.cadence.label),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (schedule.paused) {
                        Text(stringResource(R.string.recurring_paused), style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text(stringResource(
                            R.string.recurring_next_date,
                            schedule.nextDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                        ))
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        TextButton(enabled = !controls.busy, onClick = { viewModel.openEditor(schedule) }) {
                            Text(stringResource(R.string.recurring_edit))
                        }
                        TextButton(
                            enabled = !controls.busy,
                            onClick = { viewModel.setPaused(schedule.id, !schedule.paused) },
                        ) {
                            Text(stringResource(if (schedule.paused) R.string.recurring_resume else R.string.recurring_pause))
                        }
                        TextButton(enabled = !controls.busy, onClick = { pendingCancellation = schedule.id }) {
                            Text(stringResource(R.string.recurring_cancel_schedule))
                        }
                    }
                }
            }
        }
    }

    controls.editor?.let { editor ->
        RecurringExpenseSheet(
            state = state,
            schedule = editor.previous,
            onDismiss = viewModel::dismissEditor,
            onSave = viewModel::save,
        )
    }

    pendingCancellation?.let { id ->
        val schedule = state.schedules.firstOrNull { it.id == id }
        if (schedule != null) {
            AlertDialog(
                onDismissRequest = { pendingCancellation = null },
                title = { Text(stringResource(R.string.recurring_cancel_title)) },
                text = { Text(stringResource(R.string.recurring_cancel_detail, schedule.details.title)) },
                confirmButton = {
                    TextButton(onClick = {
                        pendingCancellation = null
                        viewModel.cancel(id)
                    }) { Text(stringResource(R.string.recurring_cancel_schedule)) }
                },
                dismissButton = {
                    TextButton(onClick = { pendingCancellation = null }) {
                        Text(stringResource(R.string.recurring_keep_schedule))
                    }
                },
            )
        }
    }
}

@Composable
private fun RecurringExpenseSheet(
    state: RecurringExpensesUiState,
    schedule: RecurringExpense?,
    onDismiss: () -> Unit,
    onSave: (RecurringExpenseDetails) -> Unit,
) {
    val previous = schedule?.details
    val editing = schedule != null
    var cadence by rememberSaveable(schedule?.id) { mutableStateOf(previous?.cadence ?: RecurrenceCadence.MONTHLY) }
    AddEditExpenseSheet(
        expenseToEdit = schedule?.let {
            Expense(
                id = it.id,
                title = it.details.title,
                amount = it.details.amount,
                category = state.categories.firstOrNull { category -> category.hasSameId(it.details.category) }
                    ?: it.details.category,
                date = it.details.startDate,
                notes = it.details.notes,
            )
        },
        categories = state.categories,
        currency = state.currency,
        onDismiss = onDismiss,
        onOpenAddCategory = null,
        heading = stringResource(if (editing) R.string.recurring_edit_title else R.string.recurring_add),
        actionLabel = stringResource(if (editing) R.string.recurring_save_changes else R.string.recurring_add_action),
        dateLabel = stringResource(R.string.recurring_start_date),
        isSaving = state.interaction.busy,
        additionalFields = {
            Text(stringResource(R.string.recurring_frequency), style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(RecurrenceCadence.entries) { option ->
                    FilterChip(
                        selected = cadence == option,
                        onClick = { cadence = option },
                        label = { Text(stringResource(option.label)) },
                    )
                }
            }
            state.interaction.message?.let {
                Text(stringResource(it), color = MaterialTheme.colorScheme.error)
            }
        },
        onSave = { title, amount, category, date, notes ->
            onSave(RecurringExpenseDetails(title, amount, category, date, notes, cadence))
        },
    )
}

private fun categoryName(schedule: RecurringExpense, state: RecurringExpensesUiState): String =
    state.categories.firstOrNull { it.hasSameId(schedule.details.category) }?.displayName
        ?: schedule.details.category.displayName

@get:StringRes
private val RecurrenceCadence.label: Int
    get() = when (this) {
        RecurrenceCadence.DAILY -> R.string.recurring_daily
        RecurrenceCadence.WEEKLY -> R.string.recurring_weekly
        RecurrenceCadence.MONTHLY -> R.string.recurring_monthly
        RecurrenceCadence.YEARLY -> R.string.recurring_yearly
    }
