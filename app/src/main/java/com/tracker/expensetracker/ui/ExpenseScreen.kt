package com.tracker.expensetracker.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tracker.expensetracker.R
import com.tracker.expensetracker.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(
    viewModel: ExpenseViewModel,
    budgetViewModel: BudgetViewModel,
    dataBackupViewModel: DataBackupViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val budgetState by budgetViewModel.state.collectAsStateWithLifecycle()
    var budgetsVisible by rememberSaveable { mutableStateOf(false) }
    val dashboardScroll = rememberLazyListState()
    BackHandler(enabled = budgetsVisible) { budgetsVisible = false }
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val newExpenseLabel = "New Expense"

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onUserMessageShown()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (budgetsVisible) "Budgets" else "Expense Tracker",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    TextButton(onClick = viewModel::onOpenCurrencyDialog) {
                        Text(uiState.currency.code)
                    }
                    IconButton(onClick = { viewModel.onOpenThemeDialog() }) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Appearance",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = viewModel::onOpenCategoryManager) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = "Manage categories",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    DataBackupActions(dataBackupViewModel)
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = !budgetsVisible,
                    onClick = { budgetsVisible = false },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = budgetsVisible,
                    onClick = {
                        focusManager.clearFocus()
                        budgetsVisible = true
                    },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                    label = { Text("Budgets") }
                )
            }
        },
        floatingActionButton = {
            if (!budgetsVisible) {
                ExtendedFloatingActionButton(
                    onClick = viewModel::onOpenAddExpense,
                    modifier = Modifier.semantics { contentDescription = newExpenseLabel },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(20.dp),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null
                        )
                    },
                    text = {
                        Text(
                            text = newExpenseLabel,
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (budgetsVisible) {
            BudgetScreen(
                state = budgetState,
                onSave = budgetViewModel::saveLimit,
                onRemove = budgetViewModel::removeLimit,
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            )
        } else {
            LazyColumn(
                state = dashboardScroll,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp)
            ) {
                item {
                    SummaryCard(
                        summary = uiState.summary,
                        currency = uiState.currency
                    )
                }

                budgetState.progress.firstOrNull { it.limit.categoryId == null }?.let { budget ->
                    item {
                        BudgetProgressBar("Monthly budget", budget, budgetState.currency)
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = viewModel::onSearchQueryChanged,
                            label = { Text("Search titles and notes") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            modifier = Modifier.fillMaxWidth()
                        )
                        FilterChipRow(
                            currentFilter = uiState.filter,
                            selectedCategory = uiState.selectedCategory,
                            categories = uiState.categories,
                            onDateFilterSelected = viewModel::onDateFilterSelected,
                            onCategoryFilterSelected = viewModel::onCategoryFilterSelected
                        )
                        if (uiState.hasActiveFilters) {
                            TextButton(onClick = viewModel::onClearFilters) { Text("Clear filters") }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Transactions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = pluralStringResource(
                                R.plurals.transaction_entries,
                                uiState.expenses.size,
                                uiState.expenses.size
                            ),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                if (uiState.expenses.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (uiState.hasActiveFilters) "No matching expenses" else "No expenses recorded",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(
                        items = uiState.expenses,
                        key = { it.id }
                    ) { expense ->
                        ExpenseItemCard(
                            expense = expense,
                            currency = uiState.currency,
                            onClick = { viewModel.onOpenEditExpense(expense) },
                            onDelete = { viewModel.onDeleteExpense(expense) }
                        )
                    }
                }

            }
        }

        if (uiState.isAddEditSheetOpen) {
            AddEditExpenseSheet(
                expenseToEdit = uiState.editingExpense,
                categories = uiState.categories,
                currency = uiState.currency,
                onDismiss = { viewModel.onDismissSheet() },
                onOpenAddCategory = { viewModel.onOpenAddCategory() },
                onSave = { title, amount, category, date, notes ->
                    viewModel.onSaveExpense(title, amount, category, date, notes)
                }
            )
        }

        if (uiState.isCurrencyDialogVisible) {
            CurrencySelectionDialog(
                currentCurrency = uiState.currency,
                onCurrencySelected = { viewModel.onSelectCurrency(it) },
                onDismiss = { viewModel.onDismissCurrencyDialog() }
            )
        }

        val categoryEditor = uiState.categoryEditor
        if (categoryEditor != null) {
            CategoryEditorDialog(
                state = categoryEditor,
                onNameChanged = viewModel::onCategoryNameChanged,
                onSave = viewModel::onSaveCategory,
                onDismiss = viewModel::onDismissCategoryEditor
            )
        } else if (uiState.isCategoryManagerVisible) {
            CategoryManagerDialog(
                categories = uiState.categories.filterNot { it.isBuiltIn },
                onAdd = viewModel::onOpenAddCategory,
                onEdit = viewModel::onOpenEditCategory,
                onSetArchived = viewModel::onSetCategoryArchived,
                onDismiss = viewModel::onDismissCategoryManager
            )
        }

        if (uiState.isThemeDialogVisible) {
            ThemeSelectionDialog(
                currentTheme = uiState.theme,
                currentMode = uiState.themeMode,
                onThemeSelected = { viewModel.onSelectTheme(it) },
                onModeSelected = { viewModel.onSelectThemeMode(it) },
                onDismiss = { viewModel.onDismissThemeDialog() }
            )
        }
    }
}
