package com.citadel.budget.data.local

/**
 * Default category set seeded on first launch, organized by 50/30/20 bucket.
 */
object DefaultData {

    fun categories(): List<CategoryEntity> = listOf(
        // ---- NEEDS: The Walls (50%) ----
        CategoryEntity(name = "Housing", bucket = Bucket.NEEDS, icon = "home", colorArgb = 0xFF2563EB, isDefault = true),
        CategoryEntity(name = "Groceries", bucket = Bucket.NEEDS, icon = "groceries", colorArgb = 0xFF10B981, isDefault = true),
        CategoryEntity(name = "Utilities", bucket = Bucket.NEEDS, icon = "utilities", colorArgb = 0xFF06B6D4, isDefault = true),
        CategoryEntity(name = "Transport", bucket = Bucket.NEEDS, icon = "transport", colorArgb = 0xFF6366F1, isDefault = true),
        CategoryEntity(name = "Insurance", bucket = Bucket.NEEDS, icon = "insurance", colorArgb = 0xFF0EA5E9, isDefault = true),
        CategoryEntity(name = "Health", bucket = Bucket.NEEDS, icon = "health", colorArgb = 0xFFEF4444, isDefault = true),
        // ---- WANTS: The Gardens (30%) ----
        CategoryEntity(name = "Dining Out", bucket = Bucket.WANTS, icon = "dining", colorArgb = 0xFFF59E0B, isDefault = true),
        CategoryEntity(name = "Shopping", bucket = Bucket.WANTS, icon = "shopping", colorArgb = 0xFFEC4899, isDefault = true),
        CategoryEntity(name = "Entertainment", bucket = Bucket.WANTS, icon = "fun", colorArgb = 0xFF8B5CF6, isDefault = true),
        CategoryEntity(name = "Travel", bucket = Bucket.WANTS, icon = "travel", colorArgb = 0xFF14B8A6, isDefault = true),
        CategoryEntity(name = "Hobbies", bucket = Bucket.WANTS, icon = "hobby", colorArgb = 0xFFF97316, isDefault = true),
        CategoryEntity(name = "Subscriptions", bucket = Bucket.WANTS, icon = "subs", colorArgb = 0xFF64748B, isDefault = true),
        // ---- SAVINGS: The Vault (20%) ----
        CategoryEntity(name = "Emergency Fund", bucket = Bucket.SAVINGS, icon = "ef", colorArgb = 0xFF059669, isDefault = true),
        CategoryEntity(name = "Investing", bucket = Bucket.SAVINGS, icon = "invest", colorArgb = 0xFF3B82F6, isDefault = true),
        CategoryEntity(name = "Retirement", bucket = Bucket.SAVINGS, icon = "retire", colorArgb = 0xFF7C3AED, isDefault = true),
        CategoryEntity(name = "Debt Payoff", bucket = Bucket.SAVINGS, icon = "debt", colorArgb = 0xFFDC2626, isDefault = true),
        CategoryEntity(name = "Goal Transfer", bucket = Bucket.SAVINGS, icon = "goal", colorArgb = 0xFF0D9488, isDefault = true),
        // ---- INCOME sources ----
        CategoryEntity(name = "Salary", bucket = Bucket.INCOME, icon = "salary", colorArgb = 0xFF059669, isDefault = true),
        CategoryEntity(name = "Side Hustle", bucket = Bucket.INCOME, icon = "hustle", colorArgb = 0xFFF59E0B, isDefault = true),
        CategoryEntity(name = "Other Income", bucket = Bucket.INCOME, icon = "other", colorArgb = 0xFF64748B, isDefault = true)
    )
}
