package com.receipt2report.offline.data

import kotlinx.coroutines.flow.Flow

class Repo(private val db: AppDb) {
  fun receiptRows(): Flow<List<com.receipt2report.offline.data.ReceiptRow>> = db.receipts().observeRows()
  fun receipts(): Flow<List<ReceiptEntity>> = db.receipts().observeAll()
  fun categories(): Flow<List<CategoryEntity>> = db.categories().observeAll()

  suspend fun ensureDefaults() {
    val existing = db.categories().getAll()
    if (existing.isNotEmpty()) return
    val defaults = listOf("Uncategorized","Supplies","Food","Travel","Tools","Reimbursement")
    defaults.forEach { name ->
      db.categories().upsert(CategoryEntity(id = Ids.newId(), name = name))
    }
  }

  suspend fun upsertReceipt(r: ReceiptEntity) = db.receipts().upsert(r)
  suspend fun getReceipt(id: String) = db.receipts().getById(id)
  suspend fun deleteReceipt(r: ReceiptEntity) = db.receipts().delete(r)

  suspend fun getCategoriesOnce(): List<CategoryEntity> = db.categories().getAll()
  suspend fun upsertCategory(c: CategoryEntity) = db.categories().upsert(c)

  suspend fun getReceiptList(): List<ReceiptEntity> = db.receipts().getAllNow()
}
