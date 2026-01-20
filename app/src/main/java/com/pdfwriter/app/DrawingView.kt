package com.pdfwriter.app

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class DrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paths = mutableListOf<DrawPath>()
    private var currentPath = Path()
    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    
    var penColor: Int = Color.BLACK
        set(value) {
            field = value
            paint.color = value
        }
    
    var penSize: Float = 5f
        set(value) {
            field = value
            paint.strokeWidth = value
        }
    
    var isEraserMode: Boolean = false
    
    var isScrollMode: Boolean = false // Scroll-Modus statt Zeichnen

    init {
        paint.color = penColor
        paint.strokeWidth = penSize
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw all saved paths
        for (drawPath in paths) {
            paint.color = drawPath.color
            paint.strokeWidth = drawPath.width
            canvas.drawPath(drawPath.path, paint)
        }
        
        // Draw current path
        paint.color = penColor
        paint.strokeWidth = penSize
        canvas.drawPath(currentPath, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Im Scroll-Modus keine Zeichnungen
        if (isScrollMode) {
            return false
        }
        
        val x = event.x
        val y = event.y
        
        // Support for pressure sensitivity (stylus)
        val pressure = event.pressure
        val adjustedWidth = penSize * (0.5f + pressure * 1.5f)
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Verhindere dass ScrollView das Event abfängt
                parent?.requestDisallowInterceptTouchEvent(true)
                
                if (isEraserMode) {
                    eraseAtPoint(x, y, adjustedWidth)
                } else {
                    currentPath.moveTo(x, y)
                    paint.strokeWidth = adjustedWidth
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isEraserMode) {
                    eraseAtPoint(x, y, adjustedWidth)
                } else {
                    currentPath.lineTo(x, y)
                    paint.strokeWidth = adjustedWidth
                }
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // Erlaube ScrollView wieder Events zu verarbeiten
                parent?.requestDisallowInterceptTouchEvent(false)
                
                if (!isEraserMode) {
                    paths.add(DrawPath(Path(currentPath), penColor, adjustedWidth))
                    currentPath.reset()
                }
                invalidate()
            }
        }
        return true
    }
    
    private fun eraseAtPoint(x: Float, y: Float, eraseRadius: Float) {
        val pathIterator = paths.iterator()
        while (pathIterator.hasNext()) {
            val drawPath = pathIterator.next()
            val pathMeasure = android.graphics.PathMeasure(drawPath.path, false)
            val pathLength = pathMeasure.length
            val pos = FloatArray(2)
            
            var distance = 0f
            while (distance < pathLength) {
                pathMeasure.getPosTan(distance, pos, null)
                val dx = pos[0] - x
                val dy = pos[1] - y
                val distanceToPoint = kotlin.math.sqrt(dx * dx + dy * dy)
                
                if (distanceToPoint < eraseRadius * 3) {
                    pathIterator.remove()
                    break
                }
                distance += 5f
            }
        }
    }

    fun clearDrawing() {
        paths.clear()
        currentPath.reset()
        invalidate()
    }

    fun getBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        draw(canvas)
        return bitmap
    }

    fun getPathsCopy(): List<DrawPath> {
        return paths.map { DrawPath(Path(it.path), it.color, it.width) }
    }

    fun restorePaths(pathsList: List<DrawPath>) {
        paths.clear()
        paths.addAll(pathsList.map { DrawPath(Path(it.path), it.color, it.width) })
        invalidate()
    }

    data class DrawPath(
        val path: Path,
        val color: Int,
        val width: Float
    )
}
