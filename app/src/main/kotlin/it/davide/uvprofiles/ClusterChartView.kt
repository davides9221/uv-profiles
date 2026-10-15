package it.davide.uvprofiles

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

/**
 * Grafico a barre dei tre cluster CPU.
 *
 * Per ogni cluster mostra tre livelli sovrapposti:
 *   - fondo scuro: massimo hardware (cpuinfo_max), il riferimento
 *   - barra colorata: cap attivo (scaling_max_freq)
 *   - linea chiara: frequenza corrente (scaling_cur_freq)
 *
 * I valori sono normalizzati sul massimo hardware del cluster, cosi i
 * tre cluster sono confrontabili anche se hanno scale OPP diverse
 * (A55 2.0 GHz, A75 2.5 GHz, X1 2.73 GHz).
 */
class ClusterChartView(context: Context) : View(context) {

    private var data: UvData = UvData()

    private val paintTrack = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF2A2A38.toInt()
    }
    private val paintCap = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF2E5A8A.toInt()
    }
    private val paintCur = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF4FC3F7.toInt()
        strokeWidth = dp(3f)
        style = Paint.Style.STROKE
    }
    private val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = dp(11f)
    }
    private val paintTextMuted = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF8A8AA0.toInt()
        textSize = dp(10f)
    }

    private val rect = RectF()

    fun setData(d: UvData) {
        data = d
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if ((w <= 0f) || (h <= 0f)) return

        val labelH = dp(14f)
        val chartTop = dp(2f)
        val chartBottom = h - labelH - dp(12f)
        val chartH = (chartBottom - chartTop).coerceAtLeast(dp(10f))

        val barGap = dp(8f)
        val barH = (((chartH - (barGap * 2f))) / 3f).coerceAtLeast(dp(8f))

        for (i in 0..2) {
            val top = chartTop + (i * (barH + barGap))
            val name: String
            val cur: Int
            val max: Int
            val hw: Int
            when (i) {
                0 -> { name = "LITTLE"; cur = data.littleCur; max = data.littleMax; hw = data.littleHw }
                1 -> { name = "MIDDLE"; cur = data.middleCur; max = data.middleMax; hw = data.middleHw }
                else -> { name = "PRIME"; cur = data.primeCur; max = data.primeMax; hw = data.primeHw }
            }
            val left = dp(46f)
            val right = w - dp(52f)
            val barW = (right - left).coerceAtLeast(dp(10f))

            // fondo = max hardware
            rect.left = left
            rect.top = top
            rect.right = right
            rect.bottom = top + barH
            canvas.drawRoundRect(rect, dp(3f), dp(3f), paintTrack)

            // cap attivo, normalizzato sul max hardware
            val maxFrac = if (hw > 0) (max.toFloat() / hw).coerceIn(0f, 1f) else 0f
            val capFrac = maxFrac * barW
            if (capFrac > 1f) {
                rect.left = left
                rect.top = top
                rect.right = left + capFrac
                rect.bottom = top + barH
                canvas.drawRoundRect(rect, dp(3f), dp(3f), paintCap)
            }

            // linea frequenza corrente
            val curFrac = if (hw > 0) (cur.toFloat() / hw).coerceIn(0f, 1f) else 0f
            if (cur > 0) {
                val x = left + (curFrac * barW)
                canvas.drawLine(x, top, x, top + barH, paintCur)
            }

            // etichette
            canvas.drawText(name, dp(4f), top + (barH * 0.75f), paintText)
            val mhz = "%d".format(cur / 1000)
            canvas.drawText(mhz, right + dp(4f), top + (barH * 0.75f), paintTextMuted)
        }

        // legenda
        val legendY = h - dp(3f)
        canvas.drawText("cap/hw", dp(4f), legendY, paintTextMuted)
        val caps = "${data.littleMax / 1000} / ${data.middleMax / 1000} / ${data.primeMax / 1000}"
        canvas.drawText("MHz cap: $caps", dp(58f), legendY, paintTextMuted)
    }

    private fun dp(v: Float) = v * resources.displayMetrics.density
}
