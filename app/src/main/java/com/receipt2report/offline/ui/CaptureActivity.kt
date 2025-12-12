package com.receipt2report.offline.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.receipt2report.offline.data.AppDb
import com.receipt2report.offline.data.Ids
import com.receipt2report.offline.data.Repo
import com.receipt2report.offline.data.ReceiptEntity
import com.receipt2report.offline.databinding.ActivityCaptureBinding
import com.receipt2report.offline.ocr.ReceiptOcrParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CaptureActivity : ComponentActivity() {

  private lateinit var binding: ActivityCaptureBinding
  private var imageCapture: ImageCapture? = null
  private lateinit var cameraExecutor: ExecutorService
  private lateinit var repo: Repo

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityCaptureBinding.inflate(layoutInflater)
    setContentView(binding.root)

    repo = Repo(AppDb.get(this))
    cameraExecutor = Executors.newSingleThreadExecutor()

    binding.cancelBtn.setOnClickListener { finish() }
    binding.shootBtn.setOnClickListener { takePhoto() }

    if (hasCameraPermission()) startCamera() else requestCameraPermission()
  }

  override fun onDestroy() {
    super.onDestroy()
    cameraExecutor.shutdown()
  }

  private fun hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

  private fun requestCameraPermission() {
    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 10)
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == 10 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      startCamera()
    } else {
      Toast.makeText(this, "Camera permission required.", Toast.LENGTH_LONG).show()
      finish()
    }
  }

  private fun startCamera() {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
    cameraProviderFuture.addListener({
      val cameraProvider = cameraProviderFuture.get()
      val preview = androidx.camera.core.Preview.Builder().build().also {
        it.setSurfaceProvider(binding.preview.surfaceProvider)
      }
      imageCapture = ImageCapture.Builder().build()

      val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
      try {
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
      } catch (e: Exception) {
        Toast.makeText(this, "Failed to start camera: ${e.message}", Toast.LENGTH_LONG).show()
      }
    }, ContextCompat.getMainExecutor(this))
  }

  private fun takePhoto() {
    val imageCapture = imageCapture ?: return
    binding.shootBtn.isEnabled = false

    val file = File(cacheDir, "receipt_${System.currentTimeMillis()}.jpg")
    val output = ImageCapture.OutputFileOptions.Builder(file).build()

    imageCapture.takePicture(
      output,
      ContextCompat.getMainExecutor(this),
      object : ImageCapture.OnImageSavedCallback {
        override fun onError(exc: ImageCaptureException) {
          binding.shootBtn.isEnabled = true
          Toast.makeText(this@CaptureActivity, "Capture failed: ${exc.message}", Toast.LENGTH_LONG).show()
        }
        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
          runOcr(file)
        }
      }
    )
  }

  private fun runOcr(file: File) {
    Toast.makeText(this, "Reading receipt (offline OCR)…", Toast.LENGTH_SHORT).show()

    val image = InputImage.fromFilePath(this, androidx.core.net.toUri(file))
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    recognizer.process(image)
      .addOnSuccessListener { visionText ->
        val raw = visionText.text ?: ""
        val parsed = ReceiptOcrParser.parse(raw)

        CoroutineScope(Dispatchers.IO).launch {
          val cats = repo.getCategoriesOnce()
          val defaultCatId = cats.firstOrNull()?.id ?: ""

          val receiptId = Ids.newId()
          val r = ReceiptEntity(
            id = receiptId,
            merchant = parsed.merchant,
            dateIso = if (parsed.dateIso.isBlank()) java.time.LocalDate.now().toString() else parsed.dateIso,
            total = parsed.total,
            tax = parsed.tax,
            tip = parsed.tip,
            categoryId = defaultCatId,
            notes = "",
            rawOcr = raw
          )
          repo.upsertReceipt(r)

          launch(Dispatchers.Main) {
            val intent = Intent(this@CaptureActivity, EditReceiptActivity::class.java)
              .putExtra(EditReceiptActivity.EXTRA_ID, receiptId)
            startActivity(intent)
            finish()
          }
        }
      }
      .addOnFailureListener { e ->
        binding.shootBtn.isEnabled = true
        Toast.makeText(this, "OCR failed: ${e.message}", Toast.LENGTH_LONG).show()
      }
  }
}
