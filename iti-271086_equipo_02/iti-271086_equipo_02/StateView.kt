package upvictoria.pm_sep_dic_2023.iti_271086.pg1u1.eq_02

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View

class StateView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var isInitialState: Boolean = false
        set(value) {
            field = value
            isInitialAndFinal = isInitialState && isFinalState
            invalidate()
        }

    var isFinalState: Boolean = false
        set(value) {
            field = value
            isInitialAndFinal = isInitialState && isFinalState
            invalidate()
        }

    var isHighlighted: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    var isInitialAndFinal: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    var stateName: String = ""

    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = Color.BLACK
    }

    private val fillPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    // Puntos específicos en las orillas del nodo
    val topPoint get() = PointF(x + width / 2f, y)
    val bottomPoint get() = PointF(x + width / 2f, y + height)
    val leftPoint get() = PointF(x, y + height / 2f)
    val rightPoint get() = PointF(x + width, y + height / 2f)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (isInitialAndFinal) {
            fillPaint.color = Color.GREEN
            paint.strokeWidth = 15f
        } else {
            fillPaint.color = if (isInitialState) Color.GREEN else Color.WHITE
            paint.strokeWidth = if (isFinalState) 15f else 5f
        }

        if (isHighlighted) {
            paint.color = Color.RED
        } else {
            paint.color = Color.BLACK
        }

        val radius = (minOf(width, height) / 2f) - paint.strokeWidth / 2
        val centerX = width / 2f
        val centerY = height / 2f

        canvas.drawCircle(centerX, centerY, radius, fillPaint)
        canvas.drawCircle(centerX, centerY, radius, paint)

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 24f
            textAlign = Paint.Align.CENTER
        }

        val textY = centerY + (textPaint.textSize / 2) - ((textPaint.descent() + textPaint.ascent()) / 2)
        canvas.drawText(stateName, centerX, textY, textPaint)
    }
}