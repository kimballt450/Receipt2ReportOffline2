package com.receipt2report.offline.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.receipt2report.offline.data.AppDb
import com.receipt2report.offline.data.Repo
import com.receipt2report.offline.databinding.ActivityReportBinding
import com.receipt2report.offline.util.money
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ReportActivity : ComponentActivity() {

  private lateinit var binding: ActivityReportBinding
  private lateinit var repo: Repo

  private var pendingExport: ExportType? = null

  enum class ExportType { PDF, CSV }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityReportBinding.inflate(layoutInflater)
    setContentView(binding.root)

    repo = Repo(AppDb.get(this))

    binding.backBtn.setOnClickListener { finish() }

    binding.exportPdfBtn.setOnClickListener {
      pendingExport = ExportType.PDF
      createDocument("receipt_report.pdf", "application/pdf")
    }
    binding.exportCsvBtn.setOnClickListener {
      pendingExport = ExportType.CSV
      createDocument("receipt_export.csv", "text/csv")
    }
  }

  private fun createDocument(defaultName: String, mime: String) {
    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
      addCategory(Intent.CATEGORY_OPENABLE)
      type = mime
      putExtra(Intent.EXTRA_TITLE, defaultName)
    }
    startActivityForResult(intent, 200)
  }

  @Deprecated("Deprecated in Java")
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == 200 && resultCode == Activity.RESULT_OK) {
      val uri = data?.data ?: return
      val type = pendingExport ?: return
      lifecycleScope.launch {
        contentResolver.openOutputStream(uri)?.use { out ->
          when(type){
            ExportType.PDF -> writePdf(out)
            ExportType.CSV -> writeCsv(out)
          }
        }
        binding.status.text = "Saved: $uri"
      }
    }
  }

  private suspend fun writeCsv(out: OutputStream) {
  val receipts = repo.getReceiptList()
  val cats = repo.getCategoriesOnce()
  val catMap = cats.associateBy({ it.id }, { it.name })

  fun esc(s: String): String {
    // Always wrap in quotes and escape any inner quotes
    return "\"" + s.replace("\"", "\"\"") + "\""
  }

  fun f2(x: Double): String = String.format(java.util.Locale.US, "%.2f", x)

  out.write("Category,Date,Merchant,Total,Tax,Tip,Notes\n".toByteArray())

  val grouped = receipts.groupBy { catMap[it.categoryId] ?: "Uncategorized" }
    .toSortedMap(String.CASE_INSENSITIVE_ORDER)

  for ((catName, list) in grouped) {
    val sorted = list.sortedByDescending { it.dateIso }
    for (r in sorted) {
      val row = StringBuilder()
        .append(esc(catName)).append(',')
        .append(r.dateIso).append(',')
        .append(esc(r.merchant)).append(',')
        .append(f2(r.total)).append(',')
        .append(f2(r.tax)).append(',')
        .append(f2(r.tip)).append(',')
        .append(esc(r.notes))
        .append('\n')
        .toString()

      out.write(row.toByteArray())
    }
  }
}
  private suspend fun writePdf(out: OutputStream) {
    val receipts = repo.getReceiptList()
    val cats = repo.getCategoriesOnce()
    val catMap = cats.associateBy({ it.id }, { it.name })

    val grouped = receipts.groupBy { catMap[it.categoryId] ?: "Uncategorized" }
      .toSortedMap(String.CASE_INSENSITIVE_ORDER)

    val doc = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4-ish at 72dpi
    var page = doc.startPage(pageInfo)
    var c = page.canvas

    val paint = Paint().apply { textSize = 14f }
    var y = 40f

    c.drawText("Receipt2Report Offline — Expense Report", 40f, y, paint)
    y += 22f
    paint.textSize = 10f
    c.drawText("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), 40f, y, paint)
    y += 18f
    paint.textSize = 12f

    fun newPage() {
      doc.finishPage(page)
      val nextNum = doc.pages.size + 1
      page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, nextNum).create())
      c = page.canvas
      y = 40f
      paint.textSize = 12f
    }

    var grand = 0.0

    grouped.forEach { (catName, list) ->
      var subtotal = 0.0
      paint.textSize = 13f
      if (y > 780f) newPage()
      c.drawText(catName, 40f, y, paint)
      y += 16f

      paint.textSize = 11f
      list.sortedByDescending { it.dateIso }.forEach { r ->
        subtotal += r.total
        grand += r.total
        if (y > 800f) newPage()
        val line = "${r.dateIso}   ${r.merchant.take(28).padEnd(28)}   ${money(r.total)}"
        c.drawText(line, 40f, y, paint)
        y += 14f
      }
      paint.textSize = 12f
      if (y > 800f) newPage()
      c.drawText("Subtotal: " + money(subtotal), 40f, y, paint)
      y += 18f
    }

    paint.textSize = 14f
    if (y > 800f) newPage()
    c.drawText("GRAND TOTAL: " + money(grand), 40f, y, paint)

    doc.finishPage(page)
    doc.writeTo(out)
    doc.close()
  }
}
