package com.receipt2report.offline.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.receipt2report.offline.data.AppDb
import com.receipt2report.offline.data.Repo
import com.receipt2report.offline.data.ReceiptEntity
import com.receipt2report.offline.databinding.ActivityEditReceiptBinding
import kotlinx.coroutines.launch

class EditReceiptActivity : ComponentActivity() {

  companion object { const val EXTRA_ID = "receipt_id" }

  private lateinit var binding: ActivityEditReceiptBinding
  private lateinit var repo: Repo
  private var receiptId: String = ""
  private var selectedCategoryId: String = ""

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityEditReceiptBinding.inflate(layoutInflater)
    setContentView(binding.root)

    repo = Repo(AppDb.get(this))
    receiptId = intent.getStringExtra(EXTRA_ID) ?: ""

    setupCategoryDropdown()

    binding.viewOcrBtn.setOnClickListener {
      binding.rawOcr.visibility = if (binding.rawOcr.visibility == android.view.View.GONE) android.view.View.VISIBLE else android.view.View.GONE
    }

    binding.saveBtn.setOnClickListener {
      lifecycleScope.launch {
        val current = repo.getReceipt(receiptId) ?: return@launch
        val updated = current.copy(
          merchant = binding.merchant.text?.toString()?.trim().orEmpty(),
          dateIso = binding.date.text?.toString()?.trim().orEmpty(),
          total = binding.total.text?.toString()?.toDoubleOrNull() ?: 0.0,
          tax = binding.tax.text?.toString()?.toDoubleOrNull() ?: 0.0,
          tip = binding.tip.text?.toString()?.toDoubleOrNull() ?: 0.0,
          categoryId = selectedCategoryId.ifBlank { current.categoryId },
          notes = binding.notes.text?.toString().orEmpty()
          // category editing v1: free-text field; next step is a proper category picker CRUD
        )
        repo.upsertReceipt(updated)
        finish()
      }
    }

    lifecycleScope.launch {
      repo.getReceipt(receiptId)?.let { bind(it) }
    }
  }

private fun setupCategoryDropdown() {
  lifecycleScope.launch {
    val cats = repo.getCategoriesOnce()
    val names = cats.map { it.name }
    val a = ArrayAdapter(this@EditReceiptActivity, android.R.layout.simple_list_item_1, names)
    binding.categoryDropdown.setAdapter(a)
    binding.categoryDropdown.setOnItemClickListener { _, _, position, _ ->
      selectedCategoryId = cats[position].id
    }
    val r = repo.getReceipt(receiptId) ?: return@launch
    val idx = cats.indexOfFirst { it.id == r.categoryId }.let { if (it < 0) 0 else it }
    selectedCategoryId = cats.getOrNull(idx)?.id.orEmpty()
    binding.categoryDropdown.setText(cats.getOrNull(idx)?.name.orEmpty(), false)
  }
}

  private fun bind(r: ReceiptEntity) {
    binding.merchant.setText(r.merchant)
    binding.date.setText(r.dateIso)
    binding.total.setText(r.total.toString())
    binding.tax.setText(r.tax.toString())
    binding.tip.setText(r.tip.toString())
    
    binding.notes.setText(r.notes)
    binding.rawOcr.text = r.rawOcr
  }
}
