package com.receipt2report.offline.ui

import android.content.Intent
import com.receipt2report.offline.ui.CategoryActivity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.receipt2report.offline.data.AppDb
import com.receipt2report.offline.data.Repo
import com.receipt2report.offline.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

  private lateinit var binding: ActivityMainBinding
  private lateinit var repo: Repo
  private lateinit var adapter: ReceiptRowAdapter

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    repo = Repo(AppDb.get(this))

    adapter = ReceiptRowAdapter(
      onClick = { id ->
        startActivity(Intent(this, EditReceiptActivity::class.java).putExtra(EditReceiptActivity.EXTRA_ID, id))
      },
      onLongPress = { id ->
        lifecycleScope.launch {
          repo.getReceipt(id)?.let { repo.deleteReceipt(it) }
        }
      }
    )

    binding.list.layoutManager = LinearLayoutManager(this)
    binding.list.adapter = adapter

    binding.scanBtn.setOnClickListener {
      startActivity(Intent(this, CaptureActivity::class.java))
    }

    binding.categoryBtn.setOnClickListener {
      startActivity(Intent(this, CategoryActivity::class.java))
    }

    binding.reportBtn.setOnClickListener {
      startActivity(Intent(this, ReportActivity::class.java))
    }

    lifecycleScope.launch { repo.ensureDefaults() }

    lifecycleScope.launch {
      repo.receiptRows().collectLatest { adapter.submit(it) }
    }
  }
}
