package com.citadel.budget.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

/** The three 50/30/20 buckets, plus the income source group. */
enum class Bucket { NEEDS, WANTS, SAVINGS, INCOME }

enum class TxType { INCOME, EXPENSE, SAVINGS }

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val bucket: Bucket,
    val icon: String,
    val colorArgb: Long,
    val monthlyLimitCents: Long = 0,
    val isDefault: Boolean = false
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TxType,
    val amountCents: Long,
    val categoryId: Long,
    val note: String,
    val epochDay: Long,
    val monthKey: String,
    val createdAt: Long = System.currentTimeMillis()
)

/** A transaction together with its category (LEFT-JOIN semantics: category may be null). */
data class TxWithCategory(
    @Embedded val tx: TransactionEntity,
    @Relation(parentColumn = "categoryId", entityColumn = "id")
    val category: CategoryEntity?
)

@Entity(tableName = "bills")
data class BillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amountCents: Long,
    val dueDay: Int,
    val categoryId: Long = 0,
    val autoPay: Boolean = false,
    val lastPaidMonthKey: String = "",
    val archived: Boolean = false
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetCents: Long,
    val savedCents: Long = 0,
    val deadlineEpochDay: Long? = null,
    val isEmergencyFund: Boolean = false,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
