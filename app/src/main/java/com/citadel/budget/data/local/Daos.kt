package com.citadel.budget.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY id")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Insert
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)
}

@Dao
interface TransactionDao {
    @Transaction
    @Query("SELECT * FROM transactions WHERE monthKey = :monthKey ORDER BY epochDay DESC, createdAt DESC")
    fun observeMonth(monthKey: String): Flow<List<TxWithCategory>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE monthKey IN (:monthKeys) ORDER BY epochDay ASC")
    fun observeMonths(monthKeys: List<String>): Flow<List<TxWithCategory>>

    @Transaction
    @Query("SELECT * FROM transactions ORDER BY epochDay DESC, createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TxWithCategory>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun byId(id: Long): TransactionEntity?

    @Insert
    suspend fun insert(tx: TransactionEntity): Long

    @Update
    suspend fun update(tx: TransactionEntity)

    @Delete
    suspend fun delete(tx: TransactionEntity)

    @Query("DELETE FROM transactions WHERE monthKey = :monthKey")
    suspend fun deleteMonth(monthKey: String)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}

@Dao
interface BillDao {
    @Query("SELECT * FROM bills WHERE archived = 0 ORDER BY dueDay")
    fun observeAll(): Flow<List<BillEntity>>

    @Insert
    suspend fun insert(bill: BillEntity): Long

    @Update
    suspend fun update(bill: BillEntity)

    @Delete
    suspend fun delete(bill: BillEntity)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY isEmergencyFund DESC, createdAt")
    fun observeAll(): Flow<List<GoalEntity>>

    @Insert
    suspend fun insert(goal: GoalEntity): Long

    @Update
    suspend fun update(goal: GoalEntity)

    @Delete
    suspend fun delete(goal: GoalEntity)
}
