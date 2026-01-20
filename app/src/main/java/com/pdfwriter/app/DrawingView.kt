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
        val x = event.x
        val y = event.y
        
        // Support for pressure sensitivity (stylus)
        val pressure = event.pressure
        val adjustedWidth = penSize * (0.5f + pressure * 1.5f)
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                currentPath.moveTo(x, y)
                paint.strokeWidth = adjustedWidth
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                currentPath.lineTo(x, y)
                paint.strokeWidth = adjustedWidth
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                paths.add(DrawPath(Path(currentPath), penColor, adjustedWidth))
                currentPath.reset()
                invalidate()
            }
        }
        return super.onTouchEvent(event)
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

    private data class DrawPath(
        val path: Path,
        val color: Int,
        val width: Float
    )
}
