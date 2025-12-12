package com.receipt2report.offline.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.receipt2report.offline.data.CategoryEntity
import com.receipt2report.offline.databinding.RowCategoryBinding

class CategoryAdapter(
  private val onClick: (CategoryEntity) -> Unit,
  private val onLongPress: (CategoryEntity) -> Unit
) : RecyclerView.Adapter<CategoryVH>() {

  private var rows: List<CategoryEntity> = emptyList()

  fun submit(list: List<CategoryEntity>) {
    rows = list
    notifyDataSetChanged()
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryVH {
    val b = RowCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    return CategoryVH(b, onClick, onLongPress)
  }

  override fun onBindViewHolder(holder: CategoryVH, position: Int) = holder.bind(rows[position])
  override fun getItemCount(): Int = rows.size
}

class CategoryVH(
  private val b: RowCategoryBinding,
  private val onClick: (CategoryEntity) -> Unit,
  private val onLongPress: (CategoryEntity) -> Unit
) : RecyclerView.ViewHolder(b.root) {

  fun bind(c: CategoryEntity) {
    b.name.text = c.name
    b.sub.text = if (c.name.equals("Uncategorized", true)) "(protected)" else "Tap to rename • Long-press to delete"
    b.root.setOnClickListener { onClick(c) }
    b.root.setOnLongClickListener { onLongPress(c); true }
  }
}
