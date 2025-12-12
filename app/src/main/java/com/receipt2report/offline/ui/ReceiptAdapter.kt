package com.receipt2report.offline.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.receipt2report.offline.data.ReceiptEntity
import com.receipt2report.offline.databinding.RowReceiptBinding
import com.receipt2report.offline.util.money

class ReceiptAdapter(
  private val onClick: (String) -> Unit,
  private val onLongPress: (String) -> Unit
) : RecyclerView.Adapter<ReceiptVH>() {

  private var rows: List<ReceiptEntity> = emptyList()

  fun submit(list: List<ReceiptEntity>) {
    rows = list
    notifyDataSetChanged()
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceiptVH {
    val b = RowReceiptBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    return ReceiptVH(b, onClick, onLongPress)
  }

  override fun onBindViewHolder(holder: ReceiptVH, position: Int) = holder.bind(rows[position])
  override fun getItemCount(): Int = rows.size
}

class ReceiptVH(
  private val b: RowReceiptBinding,
  private val onClick: (String) -> Unit,
  private val onLongPress: (String) -> Unit
) : RecyclerView.ViewHolder(b.root) {

  fun bind(r: ReceiptEntity) {
    b.merchant.text = r.merchant.ifBlank { "Unknown" }
    b.total.text = money(r.total)
    b.meta.text = "${r.dateIso} • ${r.categoryId.take(8)}"
    b.root.setOnClickListener { onClick(r.id) }
    b.root.setOnLongClickListener { onLongPress(r.id); true }
  }
}
