package com.tracker.expensetracker.domain.model

internal enum class AppLockMode(val timeoutMillis: Long?) {
    OFF(null),
    IMMEDIATE(0),
    THIRTY_SECONDS(30_000),
    ONE_MINUTE(60_000),
    FIVE_MINUTES(300_000),
}

internal data class AppLockSession(
    val mode: AppLockMode? = null,
    val requiresUnlock: Boolean = true,
    private val backgroundSince: Long? = null,
) {
    val isLoading: Boolean get() = mode == null
    val isEnabled: Boolean get() = mode != null && mode != AppLockMode.OFF
    val canShowContent: Boolean get() = !isLoading && !requiresUnlock

    fun load(mode: AppLockMode): AppLockSession =
        copy(mode = mode, requiresUnlock = mode != AppLockMode.OFF)

    fun onBackground(now: Long, isConfigurationChange: Boolean): AppLockSession {
        if (isConfigurationChange) return this
        return copy(
            backgroundSince = now,
            requiresUnlock = requiresUnlock || mode == AppLockMode.IMMEDIATE,
        )
    }

    fun onForeground(now: Long): AppLockSession {
        val timeout = mode?.timeoutMillis
        val since = backgroundSince
        val expired = timeout != null && since != null && now - since >= timeout
        return copy(requiresUnlock = requiresUnlock || expired, backgroundSince = null)
    }

    fun afterAuthentication(): AppLockSession =
        copy(requiresUnlock = isEnabled && backgroundSince != null)

    fun changeMode(newMode: AppLockMode): AppLockSession {
        val locked = when {
            newMode == AppLockMode.OFF -> false
            mode == AppLockMode.OFF -> backgroundSince != null
            else -> requiresUnlock
        }
        return copy(mode = newMode, requiresUnlock = locked)
    }
}
