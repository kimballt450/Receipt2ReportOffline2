package com.receipt2report.offline.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [CategoryEntity::class, ReceiptEntity::class],
  version = 1,
  exportSchema = false
)
abstract class AppDb : RoomDatabase() {
  abstract fun categories(): CategoryDao
  abstract fun receipts(): ReceiptDao

  companion object {
    @Volatile private var INSTANCE: AppDb? = null

    fun get(context: Context): AppDb =
      INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          AppDb::class.java,
          "r2r_offline.db"
        ).build().also { INSTANCE = it }
      }
  }
}
