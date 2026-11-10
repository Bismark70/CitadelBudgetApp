package com.citadel.budget.util

import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

object Fmt {

    fun money(cents: Long, code: String = "USD", compact: Boolean = false): String {
        val nf = NumberFormat.getCurrencyInstance(Locale.US)
        runCatching { nf.currency = Currency.getInstance(code) }
        nf.maximumFractionDigits = if (compact) 0 else 2
        nf.minimumFractionDigits = if (compact) 0 else 2
        return nf.format(cents / 100.0)
    }

    fun currencySymbol(code: String): String = runCatching {
        Currency.getInstance(code).getSymbol(Locale.US)
    }.getOrDefault("$")

    fun monthTitle(key: String): String =
        YearMonth.parse(key).format(DateTimeFormatter.ofPattern("MMMM yyyy"))

    fun monthShort(key: String): String =
        YearMonth.parse(key).format(DateTimeFormatter.ofPattern("MMM"))

    fun dateTitle(epochDay: Long): String =
        LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("MMM d, yyyy"))

    fun dayShort(epochDay: Long): String =
        LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("MMM d"))

    fun monthKeyOf(date: LocalDate = LocalDate.now()): String =
        "%04d-%02d".format(date.year, date.monthValue)

    /** The last [count] month keys ending with the current month (oldest first). */
    fun lastMonthKeys(count: Int = 6): List<String> {
        val now = YearMonth.now()
        return (count - 1 downTo 0).map { now.minusMonths(it.toLong()) }
            .map { "%04d-%02d".format(it.year, it.monthValue) }
    }
}
