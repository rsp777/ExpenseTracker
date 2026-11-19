# Expense Tracker

[![Version](https://img.shields.io/badge/Version-v1.0.0-brightgreen.svg)](README.md)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-green.svg)](README.md)

A high-performance, offline-first personal expense tracking application built following modern Android engineering standards, Jetpack Compose, Material 3, Clean Architecture with Unidirectional Data Flow (UDF), and automated CI/CD pipelines.

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
- **Custom Category Management**: Create and add custom categories with custom titles, an 8-color Material color palette, and 16 distinct category icons, persisted directly in Room database alongside default categories.
- **Instant Filtering**: Filter expenses effortlessly by Today, This Week, This Month, or specific Categories (default and custom).
- **Add & Edit Transactions**: Intuitive bottom sheet input for amounts in the selected currency, categories with custom color tags and icons, dates, and optional notes.
- **Manage Transactions**: Quick editing and safe deletion with confirmation dialogs.
- **Privacy & Offline First**: 100% on-device local SQLite database; zero external network tracking.

## Building & Verification
- Run Unit Tests: `./gradlew testDebugUnitTest`
- Build Debug APK: `./gradlew assembleDebug`

## Release History & Changelog Table

| Version | Highlights & Changes | Build / Target |
|---|---|---|
| `v1.0.0` | Initial release featuring multi-currency selection (USD, EUR, GBP, INR, JPY, CAD, AUD, etc.), custom category creation with color/icon pickers, offline Room persistence with version 2 migration, Jetpack Compose Material 3 UI, UDF architecture, comprehensive unit tests, and GitHub Actions CI gateway. | Build 1 (SDK 37) |
