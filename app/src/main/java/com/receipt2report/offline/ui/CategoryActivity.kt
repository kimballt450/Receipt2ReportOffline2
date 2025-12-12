package com.receipt2report.offline.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.TextInputEditText
import com.receipt2report.offline.data.AppDb
import com.receipt2report.offline.data.CategoryEntity
import com.receipt2report.offline.data.Ids
import com.receipt2report.offline.data.Repo
import com.receipt2report.offline.databinding.ActivityCategoryBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CategoryActivity : ComponentActivity() {

  private lateinit var binding: ActivityCategoryBinding
  private lateinit var repo: Repo
  private lateinit var adapter: CategoryAdapter

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityCategoryBinding.inflate(layoutInflater)
    setContentView(binding.root)

    repo = Repo(AppDb.get(this))

    adapter = CategoryAdapter(
      onClick = { cat -> rename(cat) },
      onLongPress = { cat -> delete(cat) }
    )

    binding.list.layoutManager = LinearLayoutManager(this)
    binding.list.adapter = adapter

    binding.addBtn.setOnClickListener { add() }

    lifecycleScope.launch { repo.ensureDefaults() }
    lifecycleScope.launch {
      repo.categories().collectLatest { adapter.submit(it) }
    }
  }

  private fun add() {
    val input = TextInputEditText(this).apply { hint = "Category name" }
    AlertDialog.Builder(this)
      .setTitle("Add category")
      .setView(input)
      .setPositiveButton("Add") { _, _ ->
        val name = input.text?.toString()?.trim().orEmpty()
        if (name.isNotBlank()) {
          lifecycleScope.launch { repo.upsertCategory(CategoryEntity(id = Ids.newId(), name = name)) }
        }
      }
      .setNegativeButton("Cancel", null)
      .show()
  }

  private fun rename(cat: CategoryEntity) {
    val input = TextInputEditText(this).apply { setText(cat.name) }
    AlertDialog.Builder(this)
      .setTitle("Rename category")
      .setView(input)
      .setPositiveButton("Save") { _, _ ->
        val name = input.text?.toString()?.trim().orEmpty()
        if (name.isNotBlank()) {
          lifecycleScope.launch { repo.upsertCategory(cat.copy(name = name)) }
        }
      }
      .setNegativeButton("Cancel", null)
      .show()
  }

  private fun delete(cat: CategoryEntity) {
    if (cat.name.equals("Uncategorized", true)) return
    AlertDialog.Builder(this)
      .setTitle("Delete category?")
      .setMessage("Receipts in this category will be moved to Uncategorized.")
      .setPositiveButton("Delete") { _, _ ->
        lifecycleScope.launch { repo.deleteCategory(cat.id) }
      }
      .setNegativeButton("Cancel", null)
      .show()
  }
}
