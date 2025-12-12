package com.receipt2report.offline.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
  @PrimaryKey val id: String,
  val name: String
)

@Entity(tableName = "receipts")
data class ReceiptEntity(
  @PrimaryKey val id: String,
  val merchant: String,
  val dateIso: String,
  val total: Double,
  val tax: Double,
  val tip: Double,
  val categoryId: String,
  val notes: String,
  val rawOcr: String
)
