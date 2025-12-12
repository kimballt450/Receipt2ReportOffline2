package com.receipt2report.offline.data

import java.util.UUID

object Ids {
  fun newId(): String = UUID.randomUUID().toString()
}
