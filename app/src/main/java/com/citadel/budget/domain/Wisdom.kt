package com.citadel.budget.domain

/**
 * "Fortress Wisdom" — principles distilled from five best-selling money books.
 * These are paraphrased principles (with attribution), used to power the rotating
 * wisdom card and the coaching nudges across the app.
 *
 *  - The Total Money Makeover      (Dave Ramsey)     -> Baby Steps tracker, starter fund first
 *  - Get Good with Money           (Tiffany Aliche)  -> Survival Number, money-leak alerts
 *  - I Will Teach You to Be Rich   (Ramit Sethi)     -> conscious spending, custom splits
 *  - The Psychology of Money       (Morgan Housel)   -> savings-gap scoring
 *  - Broke Millennial              (Erin Lowry)      -> shame-free tone everywhere
 */
data class WisdomCard(val principle: String, val source: String, val icon: String)

object Wisdom {

    val cards: List<WisdomCard> = listOf(
        WisdomCard(
            "Small walls first: save a starter emergency fund before you attack anything else.",
            "Baby Step 1 · The Total Money Makeover", "shield"
        ),
        WisdomCard(
            "Live like no one else now, so later you can live and give like no one else.",
            "Dave Ramsey · The Total Money Makeover", "flag"
        ),
        WisdomCard(
            "Automate your money — bills, savings, investing. Systems beat willpower every month.",
            "I Will Teach You to Be Rich · Ramit Sethi", "bolt"
        ),
        WisdomCard(
            "Spend extravagantly on the things you love, and cut mercilessly on the things you don't.",
            "Conscious Spending · Ramit Sethi", "star"
        ),
        WisdomCard(
            "Wealth is what you don't see. The flashy stuff is bought with money that's now gone.",
            "The Psychology of Money · Morgan Housel", "vault"
        ),
        WisdomCard(
            "Doing well with money has little to do with how smart you are — and everything to do with how you behave.",
            "The Psychology of Money · Morgan Housel", "trend"
        ),
        WisdomCard(
            "Protect the gap between your ego and your income. That gap is your fortress.",
            "The Psychology of Money · Morgan Housel", "trend"
        ),
        WisdomCard(
            "Separate needs from wants honestly. A budget is only as strong as its walls.",
            "Get Good with Money · Tiffany Aliche", "home"
        ),
        WisdomCard(
            "Plug your money leaks: small recurring drips sink big ships.",
            "Get Good with Money · Tiffany Aliche", "drop"
        ),
        WisdomCard(
            "Know your Survival Number — the true monthly cost of your bare-needs life.",
            "Get Good with Money · Tiffany Aliche", "ef"
        ),
        WisdomCard(
            "Money shame keeps you stuck. Face your numbers without judgment — today is day one.",
            "Broke Millennial · Erin Lowry", "heart"
        ),
        WisdomCard(
            "Pay yourself first: move money to savings the day it arrives, not the day it's left.",
            "The classic rule all five books agree on", "vault"
        )
    )

    fun forToday(dayOfYear: Int = java.time.LocalDate.now().dayOfYear): WisdomCard =
        cards[((dayOfYear % cards.size) + cards.size) % cards.size]
}
