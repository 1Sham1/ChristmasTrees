package com.example.christmastrees.ui.components

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import org.osmdroid.api.IGeoPoint
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay
import kotlin.math.max

class LabelOverlay(
    private val point: IGeoPoint,
    private val label: String,
    private val quantity: Int
) : Overlay() {

    private val textBounds = Rect()

    override fun draw(canvas: Canvas, projection: Projection) {
        val screenPos = projection.toPixels(point, null) ?: return

        // Measure text
        val textPaint = Paint().apply {
            textSize = 22f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        textPaint.getTextBounds(label, 0, label.length, textBounds)
        val textWidth = textBounds.width()
        val textHeight = textBounds.height()

        val centerX = screenPos.x.toFloat()
        val textY = screenPos.y.toFloat() - 120f

        // Draw black text, no background
        val labelPaint = Paint().apply {
            textSize = 22f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            color = Color.parseColor("#000000")
        }
        canvas.drawText(
            label,
            centerX,
            textY,
            labelPaint
        )

        // Draw quantity badge if quantity > 1
        if (quantity > 1) {
            val badgeRadius = 12f
            val badgeX = centerX + textWidth / 2f + 4f
            val badgeY = textY - 12f

            val badgePaint = Paint().apply {
                color = Color.parseColor("#C62828")
                isAntiAlias = true
            }
            canvas.drawCircle(badgeX, badgeY, badgeRadius, badgePaint)

            val badgeTextPaint = Paint().apply {
                color = Color.WHITE
                textSize = 12f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(
                quantity.toString(),
                badgeX,
                badgeY + 4f,
                badgeTextPaint
            )
        }
    }
}
