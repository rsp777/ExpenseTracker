package com.tracker.expensetracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RecurrenceCadenceTest {
    @Test
    fun monthlyCadenceRetainsTheOriginalMonthEndAnchor() {
        val anchor = LocalDate.of(2026, 1, 31)

        assertEquals(LocalDate.of(2026, 2, 28), RecurrenceCadence.MONTHLY.firstOnOrAfter(anchor, anchor.plusDays(1)))
        assertEquals(LocalDate.of(2026, 3, 31), RecurrenceCadence.MONTHLY.firstOnOrAfter(anchor, LocalDate.of(2026, 3, 1)))
        assertEquals(LocalDate.of(2026, 4, 30), RecurrenceCadence.MONTHLY.firstOnOrAfter(anchor, LocalDate.of(2026, 4, 1)))
    }

    @Test
    fun yearlyLeapDayReturnsToFebruary29WhenPossible() {
        val anchor = LocalDate.of(2024, 2, 29)

        assertEquals(LocalDate.of(2025, 2, 28), RecurrenceCadence.YEARLY.firstOnOrAfter(anchor, LocalDate.of(2025, 1, 1)))
        assertEquals(LocalDate.of(2028, 2, 29), RecurrenceCadence.YEARLY.firstOnOrAfter(anchor, LocalDate.of(2028, 1, 1)))
    }

    @Test
    fun dailyAndWeeklyCadencesRespectFutureAndExactDates() {
        val anchor = LocalDate.of(2026, 11, 28)

        assertEquals(anchor, RecurrenceCadence.DAILY.firstOnOrAfter(anchor, anchor.minusDays(1)))
        assertEquals(anchor.plusDays(3), RecurrenceCadence.DAILY.firstOnOrAfter(anchor, anchor.plusDays(3)))
        assertEquals(anchor.plusWeeks(2), RecurrenceCadence.WEEKLY.firstOnOrAfter(anchor, anchor.plusDays(8)))
        assertEquals(anchor.plusWeeks(1), RecurrenceCadence.WEEKLY.firstOnOrAfter(anchor, anchor.plusWeeks(1)))
    }
}
