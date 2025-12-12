package com.receipt2report.offline.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.receipt2report.offline.data.ReceiptRow
import com.receipt2report.offline.databinding.RowReceiptBinding
import com.receipt2report.offline.util.money

class ReceiptRowAdapter(
  private val onClick: (String) -> Unit,
  private val onLongPress: (String) -> Unit
) : RecyclerView.Adapter<ReceiptRowVH>() {

  private var rows: List<ReceiptRow> = emptyList()

  fun submit(list: List<ReceiptRow>) {
    rows = list
    notifyDataSetChanged()
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceiptRowVH {
    val b = RowReceiptBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    return ReceiptRowVH(b, onClick, onLongPress)
  }

  override fun onBindViewHolder(holder: ReceiptRowVH, position: Int) = holder.bind(rows[position])
  override fun getItemCount(): Int = rows.size
}

class ReceiptRowVH(
  private val b: RowReceiptBinding,
  private val onClick: (String) -> Unit,
  private val onLongPress: (String) -> Unit
) : RecyclerView.ViewHolder(b.root) {

  fun bind(r: ReceiptRow) {
    b.merchant.text = r.merchant.ifBlank { "Unknown" }
    b.total.text = money(r.total)
    b.meta.text = "${r.dateIso} • ${r.categoryName}"
    b.root.setOnClickListener { onClick(r.id) }
    b.root.setOnLongClickListener { onLongPress(r.id); true }
  }
}
