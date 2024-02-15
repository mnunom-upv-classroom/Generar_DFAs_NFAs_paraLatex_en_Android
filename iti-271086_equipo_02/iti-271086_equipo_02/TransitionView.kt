import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.view.View
import upvictoria.pm_sep_dic_2023.iti_271086.pg1u1.eq_02.StateView
import kotlin.math.*

class TransitionView(context: Context) : View(context) {
    var startX: Float = 0f
    var startY: Float = 0f
    var endX: Float = 0f
    var endY: Float = 0f
    var selfLoopCounter = 0
    var transitionValue: String = ""

    companion object {
        const val DISTANCE_BETWEEN_TRANSITIONS = 150f // Ajusta este valor según sea necesario
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val paint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 7f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        // Dibujo del bucle o transición regular
        if (startX == endX && startY == endY) {
            drawSelfLoop(canvas, paint)
        } else {
            drawTransition(canvas, paint)
        }

        // Dibujo del texto de la transición
        drawTransitionText(canvas)
    }

    private fun drawTransition(canvas: Canvas, paint: Paint) {
        val path = Path()
        path.moveTo(startX, startY)

        val midX = (startX + endX) / 2
        val midY = (startY + endY) / 2
        val deltaX = endX - startX
        val deltaY = endY - startY
        val angle = atan2(deltaY, deltaX)

        // Ajustar el punto de control para cambiar el inicio de la curvatura
        val controlX = startX + sin(angle) * (DISTANCE_BETWEEN_TRANSITIONS / 2) // Ajusta este valor según sea necesario
        val controlY = startY - cos(angle) * (DISTANCE_BETWEEN_TRANSITIONS / 2) // Ajusta este valor según sea necesario

        // Calcular un nuevo punto final que esté 50 unidades antes del punto de destino real
        val lineLength = sqrt(deltaX.pow(2) + deltaY.pow(2))
        val reducedLength = lineLength - 105f
        val ratio = reducedLength / lineLength

        val newEndX = startX + (endX - startX) * ratio
        val newEndY = startY + (endY - startY) * ratio

        path.quadTo(controlX, controlY, newEndX, newEndY)
        canvas.drawPath(path, paint)

        // Calcular el ángulo de la línea final
        val arrowAngle = atan2(newEndY - startY, newEndX - startX)

        // Dibujo de la cabeza de la flecha en el nuevo punto final
        drawArrowHead(canvas, paint, newEndX, newEndY, arrowAngle.toDouble())
    }


    private fun drawArrowHead(canvas: Canvas, paint: Paint, endX: Float, endY: Float, angle: Double) {
        val arrowLength = 30f // Longitud de las líneas que forman la cabeza de la flecha
        val arrowWidthAngle = PI / 6 // Ángulo para la anchura de la base de la cabeza de la flecha

        // Calcula los puntos para la base de la flecha
        val x1 = endX - arrowLength * cos(angle - arrowWidthAngle)
        val y1 = endY - arrowLength * sin(angle - arrowWidthAngle)
        val x2 = endX - arrowLength * cos(angle + arrowWidthAngle)
        val y2 = endY - arrowLength * sin(angle + arrowWidthAngle)

        // Dibuja los lados de la flecha
        canvas.drawLine(endX, endY, x1.toFloat(), y1.toFloat(), paint)
        canvas.drawLine(endX, endY, x2.toFloat(), y2.toFloat(), paint)
        // Opcional: Dibuja la base de la flecha si deseas que sea una cabeza de flecha cerrada
        canvas.drawLine(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat(), paint)
    }

    private fun drawSelfLoop(canvas: Canvas, paint: Paint) {
        val radius = 50 + 20 * selfLoopCounter
        val loopStartX = startX
        val loopStartY = startY - radius
        val path = Path()
        path.addCircle(loopStartX, loopStartY, radius.toFloat(), Path.Direction.CW)
        canvas.drawPath(path, paint)

        // Calcular el ángulo de la línea del bucle
        val arrowAngle = atan2(radius.toDouble(), 0.0) // Ángulo recto

        // Calcular la posición de la terminación de la flecha en el bucle
        val arrowEndX = loopStartX + radius * cos(arrowAngle).toFloat()
        val arrowEndY = loopStartY + radius * sin(arrowAngle).toFloat()

        drawArrowHead(canvas, paint, arrowEndX, arrowEndY, arrowAngle)
    }

    private fun drawTransitionText(canvas: Canvas) {
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 30f
            textAlign = Paint.Align.CENTER
        }

        val midX = (startX + endX) / 2
        val midY = (startY + endY) / 2
        val deltaX = endX - startX
        val deltaY = endY - startY
        val angle = atan2(deltaY, deltaX)

        // Calcular un punto a lo largo de la línea de transición para el texto
        val textPosX = midX + sin(angle) * (DISTANCE_BETWEEN_TRANSITIONS / 3)// Posición ajustada
        val textPosY = midY - cos(angle) * (DISTANCE_BETWEEN_TRANSITIONS / 3)

        canvas.drawText(transitionValue, textPosX, textPosY, textPaint)
    }


    fun setupSelfLoop(fromState: StateView, toState: StateView, value: String) {
        val (startPoint, endPoint) = determineClosestPoints(fromState, toState)
        this.startX = startPoint.x
        this.startY = startPoint.y
        this.endX = endPoint.x
        this.endY = endPoint.y
        this.transitionValue = value
        invalidate()
    }

    private fun determineClosestPoints(fromState: StateView, toState: StateView): Pair<PointF, PointF> {
        val fromPoints = listOf(fromState.topPoint, fromState.bottomPoint, fromState.leftPoint, fromState.rightPoint)
        val toPoints = listOf(toState.topPoint, toState.bottomPoint, toState.leftPoint, toState.rightPoint)

        var minDistance = Float.MAX_VALUE
        var closestPair: Pair<PointF, PointF> = Pair(fromPoints[0], toPoints[0])

        for (fromPoint in fromPoints) {
            for (toPoint in toPoints) {
                val distance = PointF.length(fromPoint.x - toPoint.x, fromPoint.y - toPoint.y)
                if (distance < minDistance) {
                    minDistance = distance
                    closestPair = Pair(fromPoint, toPoint)
                }
            }
        }

        return closestPair
    }
}