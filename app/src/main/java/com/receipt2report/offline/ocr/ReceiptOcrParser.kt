package com.receipt2report.offline.ocr

import java.util.Locale

data class OcrResult(
  val merchant: String,
  val dateIso: String,
  val total: Double,
  val tax: Double,
  val tip: Double,
  val raw: String
)

/**
 * Very simple heuristics:
 * - Merchant: first non-empty line (letters)
 * - Date: first match yyyy-mm-dd or mm/dd/yyyy
 * - Total: looks for TOTAL / AMOUNT DUE; falls back to biggest money value
 * - Tax: looks for TAX
 * - Tip: looks for TIP
 */
object ReceiptOcrParser {

  fun parse(rawText: String): OcrResult {
    val lines = rawText
      .split("\n")
      .map { it.trim() }
      .filter { it.isNotBlank() }

    val merchant = lines.firstOrNull { it.any { ch -> ch.isLetter() } }?.take(40) ?: "Unknown"

    val dateIso = findDate(lines) ?: ""

    val tax = findMoneyAfterKeyword(lines, listOf("tax")) ?: 0.0
    val tip = findMoneyAfterKeyword(lines, listOf("tip","gratuity")) ?: 0.0

    val total = findMoneyAfterKeyword(lines, listOf("total","amount due","balance due","grand total"))
      ?: findBiggestMoney(lines)
      ?: 0.0

    return OcrResult(
      merchant = merchant,
      dateIso = dateIso,
      total = total,
      tax = tax,
      tip = tip,
      raw = rawText
    )
  }

  private fun findDate(lines: List<String>): String? {
    // yyyy-mm-dd
    val iso = Regex("\b(20\d{2})[-/.](0?\d|1[0-2])[-/.]([0-2]?\d|3[01])\b")
    for (l in lines) {
      val m = iso.find(l) ?: continue
      val (y, mo, d) = m.groupValues.drop(1)
      return "%04d-%02d-%02d".format(Locale.US, y.toInt(), mo.toInt(), d.toInt())
    }
    // mm/dd/yyyy
    val us = Regex("\b(0?\d|1[0-2])[/-]([0-2]?\d|3[01])[/-](20\d{2})\b")
    for (l in lines) {
      val m = us.find(l) ?: continue
      val (mo, d, y) = m.groupValues.drop(1)
      return "%04d-%02d-%02d".format(Locale.US, y.toInt(), mo.toInt(), d.toInt())
    }
    return null
  }

  private fun findMoneyAfterKeyword(lines: List<String>, keywords: List<String>): Double? {
    val money = Regex("(-?\$?\s*\d{1,4}(?:[\.,]\d{2})?)")
    for (l in lines) {
      val lower = l.lowercase(Locale.US)
      if (keywords.any { lower.contains(it) }) {
        val m = money.findAll(l).toList()
        // pick last number on that line
        val last = m.lastOrNull()?.value ?: continue
        return cleanMoney(last)
      }
    }
    return null
  }

  private fun findBiggestMoney(lines: List<String>): Double? {
    val money = Regex("\$?\s*\d{1,4}(?:[\.,]\d{2})")
    var best: Double? = null
    for (l in lines) {
      for (m in money.findAll(l)) {
        val v = cleanMoney(m.value)
        if (best == null || v > best!!) best = v
      }
    }
    return best
  }

  private fun cleanMoney(s: String): Double {
    val t = s.replace("$","").replace(",","").trim()
    return t.toDoubleOrNull() ?: 0.0
  }
}
