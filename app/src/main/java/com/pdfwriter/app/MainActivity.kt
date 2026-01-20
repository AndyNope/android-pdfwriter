package com.pdfwriter.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.barteksc.pdfviewer.PDFView
import com.pdfwriter.app.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentPdfUri: Uri? = null
    private var currentPdfPath: String? = null

    private val openPdfLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { loadPdf(it) }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            savePdf()
        } else {
            Toast.makeText(this, R.string.permission_required, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                loadPdf(uri)
            }
        }
    }

    private fun setupViews() {
        binding.btnOpenPdf.setOnClickListener {
            openPdfLauncher.launch("application/pdf")
        }

        binding.btnSavePdf.setOnClickListener {
            checkPermissionAndSave()
        }

        binding.btnClear.setOnClickListener {
            binding.drawingView.clearDrawing()
        }

        binding.seekBarPenSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.drawingView.penSize = (progress + 1).toFloat()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Set initial pen size
        binding.drawingView.penSize = 5f
        binding.seekBarPenSize.progress = 4
    }

    private fun loadPdf(uri: Uri) {
        currentPdfUri = uri
        
        binding.pdfView.fromUri(uri)
            .enableSwipe(true)
            .swipeHorizontal(false)
            .enableDoubletap(true)
            .defaultPage(0)
            .enableAnnotationRendering(true)
            .password(null)
            .scrollHandle(null)
            .enableAntialiasing(true)
            .spacing(0)
            .onLoad { numPages ->
                Toast.makeText(this, "PDF geladen: $numPages Seiten", Toast.LENGTH_SHORT).show()
            }
            .onError { throwable ->
                Toast.makeText(this, "Fehler beim Laden: ${throwable.message}", Toast.LENGTH_SHORT).show()
            }
            .load()
    }

    private fun checkPermissionAndSave() {
        if (currentPdfUri == null) {
            Toast.makeText(this, R.string.no_pdf_loaded, Toast.LENGTH_SHORT).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ doesn't need storage permission for app-specific directory
            savePdf()
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                savePdf()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun savePdf() {
        try {
            // Create a bitmap from the PDF view and drawing overlay
            val bitmap = createCombinedBitmap()
            
            // Save as PDF
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            
            val canvas = page.canvas
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            pdfDocument.finishPage(page)
            
            // Save to Downloads folder
            val fileName = "annotated_${System.currentTimeMillis()}.pdf"
            val file = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
            } else {
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
            }
            
            FileOutputStream(file).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            pdfDocument.close()
            
            Toast.makeText(this, "${getString(R.string.saved_successfully)}\n${file.absolutePath}", Toast.LENGTH_LONG).show()
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "${getString(R.string.save_failed)}: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun createCombinedBitmap(): Bitmap {
        val width = binding.pdfContainer.width
        val height = binding.pdfContainer.height
        
        val combinedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(combinedBitmap)
        
        // Draw white background
        canvas.drawColor(Color.WHITE)
        
        // Draw PDF view
        binding.pdfView.draw(canvas)
        
        // Draw annotations on top
        binding.drawingView.draw(canvas)
        
        return combinedBitmap
    }
}
