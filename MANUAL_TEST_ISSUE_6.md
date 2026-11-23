# Manual test for issue #6

[Issue #6](https://github.com/rsp777/ExpenseTracker/issues/6) adds date selection to the add and edit expense forms.

## Setup

- Install the debug build from `dev-feat/6-expense-dates`. Check the toolbar for `v1.0.1-dev.<commit>`.
- Use an expense named `QA Date #6` for this checklist. Its unique title makes it easy to distinguish from existing transactions.

| ID | Steps | Expected result |
| --- | --- | --- |
| 6.1 | Tap **New Expense** and look at **Date**. Open the picker. | The field shows today's device date, and the picker opens with that date selected. |
| 6.2 | Enter `QA Date #6` and an amount of `12`. In **Date**, select a day from last month, tap **OK**, and save. Check **All**, **Today**, and **This Month**. | The expense appears exactly once in **All** with the chosen date. It is absent from **Today** and **This Month**. |
| 6.3 | Find the expense under **All** and edit it. Open **Date**, confirm the saved date is selected, choose today, and tap **Save Changes**. | The same expense appears once under **Today** with its original title and amount. |
| 6.4 | Reopen the expense, select a different date in the picker, and tap **Cancel**. Save and reopen the expense. | The form and saved transaction still show the previously saved date. |
| 6.5 | Close and reopen the app. On a test device, change the time zone and reopen the app again. Restore the original time zone after checking. | The expense card still shows the same saved calendar date after each reopen. |
| 6.6 | In the add or edit form, open the keyboard and scroll through the form on a small screen. | The date control and the save action can be reached by scrolling. |

## Automated regression

The ViewModel tests verify that saving a new expense forwards the selected date and that editing an expense updates its date, the **Today** filter, and the visible transaction count.

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.tracker.expensetracker.ui.ExpenseViewModelTest.onSaveExpense_*"
```
