package com.citadel.budget.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter fun bucketToString(value: Bucket): String = value.name
    @TypeConverter fun stringToBucket(value: String): Bucket = Bucket.valueOf(value)
    @TypeConverter fun txTypeToString(value: TxType): String = value.name
    @TypeConverter fun stringToTxType(value: String): TxType = TxType.valueOf(value)
}

@Database(
    entities = [
        CategoryEntity::class,
        TransactionEntity::class,
        BillEntity::class,
        GoalEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CitadelDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun billDao(): BillDao
    abstract fun goalDao(): GoalDao
}
