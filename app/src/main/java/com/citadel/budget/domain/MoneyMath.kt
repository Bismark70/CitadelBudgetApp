package com.citadel.budget.domain

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class BucketSplit(val needsCents: Long, val wantsCents: Long, val savingsCents: Long) {
    val total: Long get() = needsCents + wantsCents + savingsCents
}

object MoneyMath {

    /**
     * Splits income across the three buckets using the given percentages.
     * Any rounding remainder (a cent or two) goes to the largest bucket so the
     * split always sums exactly to income.
     */
    fun split(incomeCents: Long, needsPct: Int, wantsPct: Int, savingsPct: Int): BucketSplit {
        val safeIncome = incomeCents.coerceAtLeast(0)
        val needs = safeIncome * needsPct / 100
        val wants = safeIncome * wantsPct / 100
        val savings = safeIncome * savingsPct / 100
        val remainder = safeIncome - needs - wants - savings
        return when {
            needsPct >= wantsPct && needsPct >= savingsPct ->
                BucketSplit(needs + remainder, wants, savings)
            wantsPct >= savingsPct ->
                BucketSplit(needs, wants + remainder, savings)
            else ->
                BucketSplit(needs, wants, savings + remainder)
        }
    }

    /** Parses user input like "1,250.75" into cents. Returns null when invalid. */
    fun parseCents(text: String): Long? {
        val cleaned = text.trim().replace(",", "").replace("$", "")
        if (cleaned.isEmpty()) return null
        val parts = cleaned.split('.')
        if (parts.size > 2) return null // reject "1.2.3" style input
        val whole = parts.getOrNull(0)?.toLongOrNull() ?: return null
        val frac = when (parts.getOrNull(1)?.length) {
            null, 0 -> 0L
            1 -> parts[1].toLongOrNull() ?: return null
            else -> parts[1].take(2).toLongOrNull() ?: return null
        }
        if (whole < 0 || frac < 0) return null
        return whole * 100 + frac
    }

    /**
     * Citadel Score (0-100): the health gauge of your money fortress.
     *  - 30 pts  savings rate (a 20%+ rate earns full marks — the 50/30/20 target)
     *  - 25 pts  budget adherence (staying inside your bucket allocations)
     *  - 25 pts  emergency fund (6 months of your Survival Number = full marks)
     *  - 20 pts  goal progress (average completion across all goals)
     *
     * Inspired by Morgan Housel's "gap between ego and income" + Tiffany Aliche's
     * financial-wholeness checklist: behavior over brilliance.
     */
    fun citadelScore(
        savingsRate: Double,
        adherence: Double,
        efMonths: Double,
        goalProgress: Double
    ): Int {
        val savingsPts = 30.0 * min(1.0, max(0.0, savingsRate) / 0.20)
        val adherencePts = 25.0 * adherence.coerceIn(0.0, 1.0)
        val efPts = 25.0 * min(1.0, max(0.0, efMonths) / 6.0)
        val goalPts = 20.0 * min(1.0, goalProgress.coerceIn(0.0, 1.0))
        return (savingsPts + adherencePts + efPts + goalPts).roundToInt().coerceIn(0, 100)
    }

    fun tierFor(score: Int): String = when {
        score < 40 -> "Wooden Palisade"
        score < 60 -> "Stone Keep"
        score < 75 -> "Iron Fortress"
        score < 90 -> "Golden Citadel"
        else -> "Adamantine Stronghold"
    }

    /** One coaching nudge, weakest area first. */
    fun tipFor(savingsWeak: Boolean, adherenceWeak: Boolean, efWeak: Boolean, goalsWeak: Boolean): String = when {
        efWeak -> "Strengthen the vault: grow your emergency fund toward 3–6 months of your Survival Number."
        savingsWeak -> "Widen the gap: saving even 1% more each month hardens your walls."
        adherenceWeak -> "A wall is breached — check the over-budget bucket below and plug the leak."
        goalsWeak -> "Feed your goals: small, steady contributions keep the drawbridge up."
        else -> "Your fortress holds strong. Keep the gates shut and the habits steady."
    }
}
