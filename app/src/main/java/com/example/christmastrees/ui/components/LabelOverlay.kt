package com.example.christmastrees.ui.components

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.example.christmastrees.data.MarkerSize
import org.osmdroid.api.IGeoPoint
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay

class LabelOverlay(
    private val point: IGeoPoint,
    private val label: String,
    private val quantity: Int,
    private val markerSize: MarkerSize = MarkerSize.NORMAL
) : Overlay() {

    // quantity is kept for backward compatibility but not used in rendering

    private val textBounds = Rect()

    override fun draw(canvas: Canvas, projection: Projection) {
        val screenPos = projection.toPixels(point, null) ?: return

        // Measure text
        val measurePaint = Paint().apply {
            textSize = 20f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
        }
        measurePaint.getTextBounds(label, 0, label.length, textBounds)
        val textWidth = textBounds.width()
        val textHeight = textBounds.height()

        val centerX = screenPos.x.toFloat()
        
        // Adjust label distance based on marker size
        val labelOffset = when (markerSize) {
            MarkerSize.SMALL -> 90f
            MarkerSize.NORMAL -> 120f
            MarkerSize.BOSS -> 150f
        }
        val textY = screenPos.y.toFloat() - labelOffset

        // Draw text with white outline for better contrast
        val outlinePaint = Paint().apply {
            textSize = 20f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            color = android.graphics.Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 6f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
        }
        val textPaint = Paint().apply {
            textSize = 20f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            color = android.graphics.Color.parseColor("#1B5E20")
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
        }
        canvas.drawText(label, centerX, textY, outlinePaint)
        canvas.drawText(label, centerX, textY, textPaint)
    }
}
