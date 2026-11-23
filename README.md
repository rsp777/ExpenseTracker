# Expense Tracker

[![Latest release](https://img.shields.io/badge/Release-v1.0.0-brightgreen.svg)](https://github.com/rsp777/ExpenseTracker/releases/tag/v1.0.0)
[![Development version](https://img.shields.io/badge/Dev-v1.0.1-orange.svg)](https://github.com/rsp777/ExpenseTracker/tree/dev)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-green.svg)](README.md)

Expense Tracker is an offline-first Android app for recording expenses and reviewing spending. It uses Jetpack Compose, Material 3, and Room.

The current `dev` build uses version code `2` and displays `1.0.1-dev.<commit>` in the app. The latest published release is `v1.0.0`.

## Architecture & Technology Stack
- UI Framework: Modern Jetpack Compose with Material 3.
- Pattern: Unidirectional Data Flow (UDF / MVI / MVVM) with deterministic StateFlow state exposure.
- Persistence: Room Database with Coroutines (Flow / suspend functions) and SharedPreferences for user preferences.
- Threading: Heavy I/O and DB operations strictly off the Main thread (Dispatchers.IO).
- Edge-to-Edge: Full edge-to-edge support with transparent system bars and responsive insets.
- Testing: Comprehensive Unit tests (testDebugUnitTest) for repository and ViewModel logic using kotlinx-coroutines-test and mockk.
- CI/CD Automation: GitHub Actions automated quality gate with dynamic version extraction, automated Git release tagging, and GitHub Release generation.

## Features
- **Dashboard & Spending Summary**: Real-time spending overview, transaction count, and identification of the highest spending category.
- **Custom Modernized Theme Engine**: Comprehensive appearance customization featuring:
  - 6 Material 3 color palettes: **Material Dynamic** (Android 12+ wallpaper sync), **Emerald Mint**, **Sapphire Blue**, **Amethyst Violet**, **Sunset Copper**, and **Midnight Obsidian**.
  - 3 Appearance Modes: **Auto (System)**, **Light Mode**, and **Dark Mode** with full tonal surface elevation and contrast.
  - Persistent preference saving in SharedPreferences so your chosen theme and mode survive app restarts.
- **Multi-Currency Selection**: Select your preferred currency (USD $, EUR €, GBP £, INR ₹, JPY ¥, CAD CA$, AUD AU$, CHF CHF, CNY ¥, BRL R$) with immediate localized formatting across all cards, sheets, and summaries.
- Create custom categories with colors and icons, edit them, and archive or restore them. Archived categories remain available in history and filters, and the new-expense picker shows active categories. [Issue #7](https://github.com/rsp777/ExpenseTracker/issues/7)
- Search expense titles and notes offline. Combine the query with date and category filters and see totals and transaction counts for the matching entries. [Issue #8](https://github.com/rsp777/ExpenseTracker/issues/8)
- Set the purchase date when adding an expense and correct it later. The date picker defaults to today for a new expense and to the saved date when editing. [Issue #6](https://github.com/rsp777/ExpenseTracker/issues/6)
- **Manage Transactions**: Quick editing and safe deletion with confirmation dialogs.
- **Privacy & Offline First**: 100% on-device local SQLite database; zero external network tracking.

## Building & Verification

Run the debug build and unit tests from the project root.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

On macOS or Linux, use `./gradlew` with the same tasks. The debug APK is at `app/build/outputs/apk/debug/app-debug.apk`.

## Release History

| Version | Highlights | Android build |
| --- | --- | --- |
| `1.0.1-dev.<commit>` | Expense date editing, category editing and archiving, and combined transaction search and filters. Fixes category overwrites, punctuation in category names, invalid amounts, and duplicate saves. Room schema 3 migrates existing categories. [Persistence fixes](https://github.com/rsp777/ExpenseTracker/issues/9) | Version code 2, target SDK 37 |
| [`v1.0.0`](https://github.com/rsp777/ExpenseTracker/releases/tag/v1.0.0) | Initial release with multi-currency display, custom category creation, offline Room storage, and Material 3 themes. | Version code 1, target SDK 37 |
