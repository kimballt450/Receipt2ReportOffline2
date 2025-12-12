package com.receipt2report.offline.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
  @Query("SELECT * FROM categories ORDER BY name ASC")
  fun observeAll(): Flow<List<CategoryEntity>>

  @Query("SELECT * FROM categories ORDER BY name ASC")
  suspend fun getAll(): List<CategoryEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(cat: CategoryEntity)

  @Delete
  suspend fun delete(cat: CategoryEntity)
}

@Dao
data class ReceiptRow(
  val id: String,
  val merchant: String,
  val dateIso: String,
  val total: Double,
  val categoryName: String
)

interface ReceiptDao {
  @Query("""
  SELECT r.id AS id, r.merchant AS merchant, r.dateIso AS dateIso, r.total AS total,
         COALESCE(c.name, 'Uncategorized') AS categoryName
  FROM receipts r
  LEFT JOIN categories c ON c.id = r.categoryId
  ORDER BY r.dateIso DESC
""")
fun observeRows(): Flow<List<ReceiptRow>>

@Query("SELECT * FROM receipts ORDER BY dateIso DESC")
fun observeAll(): Flow<List<ReceiptEntity>>

  @Query("SELECT * FROM receipts ORDER BY dateIso DESC")
  suspend fun getAllNow(): List<ReceiptEntity>

  @Query("SELECT * FROM receipts WHERE id = :id LIMIT 1")
  suspend fun getById(id: String): ReceiptEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(r: ReceiptEntity)

  @Delete
  suspend fun delete(r: ReceiptEntity)

  @Query("UPDATE receipts SET categoryId = :toCategoryId WHERE categoryId = :fromCategoryId")
  suspend fun reassignCategory(fromCategoryId: String, toCategoryId: String)
}
