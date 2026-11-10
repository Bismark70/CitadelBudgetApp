package com.citadel.budget

import com.citadel.budget.domain.MoneyMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyMathTest {

    @Test
    fun `split always sums exactly to income`() {
        val incomes = listOf(0L, 1L, 99L, 123456L, 5_000_000L)
        val splits = listOf(
            Triple(50, 30, 20),
            Triple(60, 20, 20),
            Triple(70, 20, 10),
            Triple(34, 33, 33)
        )
        incomes.forEach { income ->
            splits.forEach { (n, w, s) ->
                val result = MoneyMath.split(income, n, w, s)
                assertEquals("income=$income split=$n/$w/$s", income, result.total)
            }
        }
    }

    @Test
    fun `classic 50 30 20 on round income`() {
        val result = MoneyMath.split(3_000_00L, 50, 30, 20)
        assertEquals(1_500_00L, result.needsCents)
        assertEquals(900_00L, result.wantsCents)
        assertEquals(600_00L, result.savingsCents)
    }

    @Test
    fun `parse cents handles common input`() {
        assertEquals(1250L, MoneyMath.parseCents("12.5"))
        assertEquals(1250L, MoneyMath.parseCents("12.50"))
        assertEquals(1250L, MoneyMath.parseCents(" 12.50 "))
        assertEquals(120000L, MoneyMath.parseCents("1,200"))
        assertEquals(1200L, MoneyMath.parseCents("12"))
        assertEquals(12L, MoneyMath.parseCents("0.12"))
        assertNull(MoneyMath.parseCents(""))
        assertNull(MoneyMath.parseCents("abc"))
        assertNull(MoneyMath.parseCents("1.2.3"))
    }

    @Test
    fun `citadel score stays within bounds`() {
        assertEquals(0, MoneyMath.citadelScore(0.0, 0.0, 0.0, 0.0))
        assertEquals(100, MoneyMath.citadelScore(1.0, 1.0, 12.0, 1.0))
        // Savings rate above 20% does not push past 30 points
        val capped = MoneyMath.citadelScore(0.9, 1.0, 6.0, 1.0)
        assertEquals(100, capped)
        val mid = MoneyMath.citadelScore(0.10, 1.0, 3.0, 0.5)
        assert(mid in 1..99)
    }

    @Test
    fun `tiers ascend with score`() {
        assertEquals("Wooden Palisade", MoneyMath.tierFor(10))
        assertEquals("Stone Keep", MoneyMath.tierFor(45))
        assertEquals("Iron Fortress", MoneyMath.tierFor(62))
        assertEquals("Golden Citadel", MoneyMath.tierFor(80))
        assertEquals("Adamantine Stronghold", MoneyMath.tierFor(95))
    }
}
