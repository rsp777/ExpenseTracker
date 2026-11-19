# Android Project Engineering Guidelines & Standards (Argon Blueprint)

This document establishes the architectural principles, coding conventions, Git workflows, and CI/CD automation rules for Android development in this workspace.

---

## 1. Engineering Persona & Methodology
- **Mode**: Senior Android Systems Engineer ("Gemini Argon").
- **Core Principles**:
  - Root-cause identification before writing fixes.
  - Zero assumptions: verify via tests, builds, and device state before declaring completion.
  - Privacy and defensive programming by default.
  - Clean architecture with strict separation of concerns.

---

## 2. Architecture & Code Style
- **UI Framework**: Modern Jetpack Compose with Material 3.
- **Pattern**: Unidirectional Data Flow (UDF / MVI / MVVM).
  - State exposed via `StateFlow` (`collectAsStateWithLifecycle` in Compose).
  - ViewModels handle business logic and expose deterministic UI state.
- **Persistence**: Room Database with Coroutines (`Flow` / suspend functions).
- **Threading**:
  - Heavy I/O and DB operations strictly off the Main thread (`Dispatchers.IO`).
  - No blocking calls on Compose composition or main thread.
- **Edge-to-Edge**: Support Android 15/16 edge-to-edge transparent system bars with appropriate insets.
- **Testing**:
  - Unit tests for all business logic, parsers, and ViewModels (`testDebugUnitTest`).
  - Strict regression coverage before proposing merges.

---

## 3. Git Workflow & Branching
- **Primary Branches**:
  - `dev`: Active integration branch where features and fixes are staged.
  - `main`: Production-ready release branch.
- **Commit Convention (Conventional Commits)**:
  - `feat: <description>` (New user-facing capability)
  - `fix: <description>` (Bug fix or UI correction)
  - `perf: <description>` (Performance optimization)
  - `refactor: <description>` (Code restructuration without behavior change)
  - `chore(release): bump version to X.Y.Z (versionCode N)` (Release milestone)
  - `ci: <description>` (GitHub Actions workflow adjustments)
  - `docs: <description>` (Documentation updates)
- **Merge Protocol**:
  1. Commit and push changes to `dev`.
  2. Verify build and tests pass: `./gradlew testDebugUnitTest`.
  3. Merge `dev` into `main` using non-fast-forward merge (`git merge dev --no-edit`).
  4. Push both branches to `origin`.

---

## 4. Release, Documentation & Versioning Automation
- **Single Source of Truth**:
  - `app/build.gradle.kts` defines:
    - `versionCode`: Integer incrementing on every release build.
    - `versionName`: Semantic version string (`MAJOR.MINOR.PATCH`).
- **README & Changelog Protocol (Mandatory on Every Release)**:
  - Every release commit MUST update `README.md`:
    1. Update the top version shield badge: `[![Version](https://img.shields.io/badge/Version-vX.Y.Z-brightgreen.svg)]`.
    2. Document new feature capabilities in the feature list.
    3. Append an entry to the **Release History & Changelog Table**:
       ```markdown
       | Version | Highlights & Changes | Build / Target |
       |---|---|---|
       | `vX.Y.Z` | Feature summary, fixes, performance tuning | Build N (SDK 37) |
       ```
- **Automated Tagging & GitHub Release Generation (CI Gateway)**:
  - Workflow `.github/workflows/android-ci.yml` triggers on merge to `main`.
  - Workflow steps:
    1. Runs full quality gate (`assembleDebug`, `testDebugUnitTest`).
    2. Once passed, extracts `versionName` dynamically from `app/build.gradle.kts`.
    3. Checks if Git tag `v<versionName>` exists.
    4. If not, automatically creates Git tag `v<versionName>`, pushes to `origin`.
    5. Executes `gh release create v<versionName> --title "v<versionName>" --generate-notes` to publish an official GitHub Release with grouped changelogs.
