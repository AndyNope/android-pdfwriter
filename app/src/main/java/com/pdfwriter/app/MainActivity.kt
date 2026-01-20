package com.pdfwriter.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.view.View
import android.widget.PopupMenu
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.pdfwriter.app.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentPdfUri: Uri? = null
    private var pdfRenderer: PdfRenderer? = null
    private var currentPage: PdfRenderer.Page? = null
    private var pdfFileDescriptor: ParcelFileDescriptor? = null
    private var currentPageIndex: Int = 0
    private val pageDrawings = mutableMapOf<Int, List<DrawingView.DrawPath>>()

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

    override fun onDestroy() {
        super.onDestroy()
        closePdfRenderer()
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        // Speichere aktuelle Zeichnungen vor dem Re-render
        saveCurrentPageDrawings()
        
        // Re-rendere die aktuelle Seite mit neuer Ausrichtung
        currentPdfUri?.let {
            renderPage(currentPageIndex)
            // Lade Zeichnungen nach dem Re-render wieder
            loadPageDrawings()
        }
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                loadPdf(uri)
            }
        }
    }

    private fun setupViews() {
        binding.btnPrevPage.setOnClickListener {
            saveCurrentPageDrawings()
            if (currentPageIndex > 0) {
                currentPageIndex--
                renderPage(currentPageIndex)
                loadPageDrawings()
            }
        }

        binding.btnNextPage.setOnClickListener {
            saveCurrentPageDrawings()
            pdfRenderer?.let { renderer ->
                if (currentPageIndex < renderer.pageCount - 1) {
                    currentPageIndex++
                    renderPage(currentPageIndex)
                    loadPageDrawings()
                }
            }
        }

        binding.btnPenMode.setOnClickListener {
            binding.drawingView.isEraserMode = false
            binding.drawingView.isScrollMode = false
            highlightActiveMode()
            updateScrollModeButton()
            Toast.makeText(this, R.string.pen_mode, Toast.LENGTH_SHORT).show()
        }

        binding.btnEraserMode.setOnClickListener {
            binding.drawingView.isEraserMode = true
            binding.drawingView.isScrollMode = false
            highlightActiveMode()
            Toast.makeText(this, R.string.eraser_mode, Toast.LENGTH_SHORT).show()
        }

        binding.btnScrollMode.setOnClickListener {
            binding.drawingView.isScrollMode = !binding.drawingView.isScrollMode
            if (binding.drawingView.isScrollMode) {
                binding.drawingView.isEraserMode = false
            }
            updateScrollModeButton()
            val message = if (binding.drawingView.isScrollMode) {
                R.string.scroll_mode_enabled
            } else {
                R.string.draw_mode_enabled
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        binding.btnUndo.setOnClickListener {
            if (binding.drawingView.undo()) {
                updateUndoRedoButtons()
            }
        }

        binding.btnRedo.setOnClickListener {
            if (binding.drawingView.redo()) {
                updateUndoRedoButtons()
            }
        }

        binding.btnColorPicker.setOnClickListener {
            showColorPickerDialog()
        }

        binding.btnMenu.setOnClickListener { view ->
            showMenu(view)
        }

        // Set initial pen size
        binding.drawingView.penSize = 5f
        binding.drawingView.isEraserMode = false
        binding.drawingView.isScrollMode = false
        binding.drawingView.onDrawingChanged = { updateUndoRedoButtons() }
        highlightActiveMode()
        updateScrollModeButton()
        updateUndoRedoButtons()
        updateColorButton()
    }

    private fun updateUndoRedoButtons() {
        binding.btnUndo.isEnabled = binding.drawingView.canUndo()
        binding.btnUndo.alpha = if (binding.drawingView.canUndo()) 1.0f else 0.3f
        binding.btnRedo.isEnabled = binding.drawingView.canRedo()
        binding.btnRedo.alpha = if (binding.drawingView.canRedo()) 1.0f else 0.3f
    }

    private fun updateScrollModeButton() {
        binding.btnScrollMode.alpha = if (binding.drawingView.isScrollMode) 1.0f else 0.5f
    }

    private fun highlightActiveMode() {
        if (binding.drawingView.isEraserMode) {
            binding.btnEraserMode.alpha = 1.0f
            binding.btnPenMode.alpha = 0.5f
        } else {
            binding.btnPenMode.alpha = 1.0f
            binding.btnEraserMode.alpha = 0.5f
        }
    }

    private fun showMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.main_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_open_pdf -> {
                    openPdfLauncher.launch("application/pdf")
                    true
                }
                R.id.menu_save_pdf -> {
                    checkPermissionAndSave()
                    true
                }
                R.id.menu_clear_page -> {
                    binding.drawingView.clearDrawing()
                    updateUndoRedoButtons()
                    true
                }
                R.id.menu_adjust_pen_size -> {
                    showPenSizeDialog()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showPenSizeDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_pen_size, null)
        val seekBar = dialogView.findViewById<SeekBar>(R.id.seekBarPenSize)
        seekBar.progress = binding.drawingView.penSize.toInt() - 1
        
        AlertDialog.Builder(this)
            .setTitle(R.string.adjust_pen_size)
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                binding.drawingView.penSize = (seekBar.progress + 1).toFloat()
            }
            .setNegativeButton("Abbrechen", null)
            .show()
        
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.drawingView.penSize = (progress + 1).toFloat()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun showColorPickerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_color_picker, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        val colors = mapOf(
            R.id.colorBlack to Color.BLACK,
            R.id.colorRed to Color.RED,
            R.id.colorBlue to Color.BLUE,
            R.id.colorGreen to Color.GREEN,
            R.id.colorYellow to Color.YELLOW,
            R.id.colorOrange to Color.parseColor("#FF9800"),
            R.id.colorPurple to Color.parseColor("#9C27B0"),
            R.id.colorPink to Color.parseColor("#E91E63"),
            R.id.colorBrown to Color.parseColor("#795548"),
            R.id.colorGray to Color.GRAY,
            R.id.colorCyan to Color.CYAN,
            R.id.colorWhite to Color.WHITE
        )
        
        colors.forEach { (viewId, color) ->
            dialogView.findViewById<View>(viewId)?.setOnClickListener {
                binding.drawingView.penColor = color
                updateColorButton()
                dialog.dismiss()
            }
        }
        
        dialog.show()
    }

    private fun updateColorButton() {
        // Zeige aktuelle Farbe auf dem Button durch Hintergrundfarbe
        binding.btnColorPicker.setBackgroundColor(binding.drawingView.penColor)
    }

    private fun saveCurrentPageDrawings() {
        // Speichere Zeichnungen für aktuelle Seite
        pageDrawings[currentPageIndex] = binding.drawingView.getPathsCopy()
    }

    private fun loadPageDrawings() {
        // Lade Zeichnungen für neue Seite
        binding.drawingView.clearDrawing()
        pageDrawings[currentPageIndex]?.let { paths ->
            binding.drawingView.restorePaths(paths)
        }
        updateUndoRedoButtons()
    }

    private fun loadPdf(uri: Uri) {
        try {
            closePdfRenderer()
            currentPdfUri = uri
            currentPageIndex = 0
            pageDrawings.clear()
            
            pdfFileDescriptor = contentResolver.openFileDescriptor(uri, "r")
            pdfFileDescriptor?.let { fd ->
                pdfRenderer = PdfRenderer(fd)
                renderPage(0)
                updatePageInfo()
                Toast.makeText(this, "PDF geladen: ${pdfRenderer?.pageCount} Seiten", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Fehler beim Laden: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun renderPage(pageIndex: Int) {
        pdfRenderer?.let { renderer ->
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) return
            
            currentPage?.close()
            currentPage = renderer.openPage(pageIndex)
            
            currentPage?.let { page ->
                // Berechne Bitmap-Größe basierend auf Bildschirmbreite
                val displayMetrics = resources.displayMetrics
                val screenWidth = displayMetrics.widthPixels
                
                // Skalierungsfaktor für bessere Qualität
                val scale = screenWidth.toFloat() / page.width.toFloat()
                val scaledWidth = (page.width * scale).toInt()
                val scaledHeight = (page.height * scale).toInt()
                
                val bitmap = Bitmap.createBitmap(
                    scaledWidth,
                    scaledHeight,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                
                binding.pdfImageView.setImageBitmap(bitmap)
                
                // Warte bis das ImageView gerendert ist, dann passe DrawingView an
                binding.pdfImageView.post {
                    val imageViewWidth = binding.pdfImageView.width
                    val imageViewHeight = binding.pdfImageView.height
                    
                    // Berechne die tatsächliche Bildgröße im ImageView
                    val drawable = binding.pdfImageView.drawable
                    if (drawable != null) {
                        val intrinsicWidth = drawable.intrinsicWidth
                        val intrinsicHeight = drawable.intrinsicHeight
                        
                        val scale = imageViewWidth.toFloat() / intrinsicWidth.toFloat()
                        val actualHeight = (intrinsicHeight * scale).toInt()
                        
                        // Setze DrawingView auf exakt dieselbe Größe
                        val layoutParams = binding.drawingView.layoutParams
                        layoutParams.width = imageViewWidth
                        layoutParams.height = actualHeight
                        binding.drawingView.layoutParams = layoutParams
                        binding.drawingView.requestLayout()
                        
                        // Setze auch die Container-Höhe
                        val containerParams = binding.pdfContainer.layoutParams
                        containerParams.height = actualHeight
                        binding.pdfContainer.layoutParams = containerParams
                    }
                }
                
                updatePageInfo()
            }
        }
    }

    private fun updatePageInfo() {
        pdfRenderer?.let { renderer ->
            binding.tvPageInfo.text = "${currentPageIndex + 1}/${renderer.pageCount}"
            binding.btnPrevPage.isEnabled = currentPageIndex > 0
            binding.btnNextPage.isEnabled = currentPageIndex < renderer.pageCount - 1
        }
    }

    private fun closePdfRenderer() {
        currentPage?.close()
        pdfRenderer?.close()
        pdfFileDescriptor?.close()
        currentPage = null
        pdfRenderer = null
        pdfFileDescriptor = null
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
            saveCurrentPageDrawings()
            
            val pdfDocument = PdfDocument()
            
            pdfRenderer?.let { renderer ->
                // Speichere alle Seiten
                for (pageIdx in 0 until renderer.pageCount) {
                    // Render PDF Seite in hoher Qualität
                    val page = renderer.openPage(pageIdx)
                    val scale = 2.0f // Höhere Auflösung für bessere Qualität
                    val bitmap = Bitmap.createBitmap(
                        (page.width * scale).toInt(),
                        (page.height * scale).toInt(),
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    val pdfWidth = page.width
                    val pdfHeight = page.height
                    page.close()
                    
                    // Erstelle kombiniertes Bitmap mit Zeichnungen
                    val combinedBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(combinedBitmap)
                    canvas.drawBitmap(bitmap, 0f, 0f, null)
                    
                    // Zeichne Annotationen für diese Seite, skaliert auf die Bitmap-Größe
                    pageDrawings[pageIdx]?.let { paths ->
                        canvas.save()
                        canvas.scale(scale, scale)
                        
                        val paint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            style = android.graphics.Paint.Style.STROKE
                            strokeJoin = android.graphics.Paint.Join.ROUND
                            strokeCap = android.graphics.Paint.Cap.ROUND
                        }
                        
                        for (drawPath in paths) {
                            paint.color = drawPath.color
                            paint.strokeWidth = drawPath.width
                            canvas.drawPath(drawPath.path, paint)
                        }
                        
                        canvas.restore()
                    }
                    
                    // Füge Seite zum PDF hinzu
                    val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, pageIdx + 1).create()
                    val pdfPage = pdfDocument.startPage(pageInfo)
                    pdfPage.canvas.drawBitmap(combinedBitmap, 0f, 0f, null)
                    pdfDocument.finishPage(pdfPage)
                    
                    bitmap.recycle()
                    combinedBitmap.recycle()
                }
            }
            
            // Save to Documents folder
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
}
