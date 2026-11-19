# Expense Tracker

[![Version](https://img.shields.io/badge/Version-v1.0.0-brightgreen.svg)](README.md)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-green.svg)](README.md)

A high-performance, offline-first personal expense tracking application built following modern Android engineering standards, Jetpack Compose, Material 3, Clean Architecture with Unidirectional Data Flow (UDF), and automated CI/CD pipelines.

## Architecture & Technology Stack
- UI Framework: Modern Jetpack Compose with Material 3.
- Pattern: Unidirectional Data Flow (UDF / MVI / MVVM) with deterministic StateFlow state exposure.
- Persistence: Room Database with Coroutines (Flow / suspend functions).
- Threading: Heavy I/O and DB operations strictly off the Main thread (Dispatchers.IO).
- Edge-to-Edge: Full edge-to-edge support with transparent system bars and responsive insets.
- Testing: Comprehensive Unit tests (testDebugUnitTest) for repository and ViewModel logic using kotlinx-coroutines-test and mockk.
- CI/CD Automation: GitHub Actions automated quality gate with dynamic version extraction, automated Git release tagging, and GitHub Release generation.

## Features
- Dashboard & Spending Summary: Real-time spending overview, transaction count, and identification of the highest spending category.
- Instant Filtering: Filter expenses effortlessly by Today, This Week, This Month, or specific Categories (Food, Transport, Shopping, Bills, Entertainment, Health, Education, Other).
- Add & Edit Transactions: Intuitive bottom sheet input for amounts, categories with custom color tags and icons, dates, and optional notes.
- Manage Transactions: Quick editing and safe deletion with confirmation dialogs.
- Privacy & Offline First: 100% on-device local SQLite database; zero external network tracking.

## Building & Verification
- Run Unit Tests: `./gradlew testDebugUnitTest`
- Build Debug APK: `./gradlew assembleDebug`

## Release History & Changelog Table

| Version | Highlights & Changes | Build / Target |
|---|---|---|
| `v1.0.0` | Initial release featuring offline Room persistence, Jetpack Compose Material 3 UI, UDF architecture, comprehensive unit tests, and GitHub Actions CI gateway. | Build 1 (SDK 37) |
