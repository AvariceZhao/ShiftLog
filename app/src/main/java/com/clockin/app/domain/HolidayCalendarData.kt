package com.clockin.app.domain

import java.time.LocalDate

data class HolidayCalendarYear(
    val year: Int,
    /** True means a rest day; false means a make-up workday. */
    val days: Map<LocalDate, Boolean>,
    val papers: List<String>,
)
