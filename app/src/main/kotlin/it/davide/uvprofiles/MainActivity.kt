package it.davide.uvprofiles

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/**
 * Activity principale: profili undervolt + sezione status con grafico.
 *
 * L'app e autonoma: non richiede alcun modulo Magisk. I profili stanno
 * nello script res/raw/uv_profiles.sh, che viene copiato su file e
 * lanciato con su. Un modulo serve solo per riapplicare il profilo al
 * boot, perche quello puo farlo solo qualcosa che gira all'avvio.
 */
class MainActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var statusBlock: LinearLayout
    private lateinit var chart: ClusterChartView

    private val profiles = listOf(
        "eco" to "ECO",
        "balanced" to "BALANCED",
        "sixcore" to "SIXCORE",
        "sport" to "SPORT",
        "off" to "OFF"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = android.widget.ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(16), dp(28), dp(16), dp(28))
            setBackgroundColor(BG)
        }
        scroll.addView(root)

        root.addView(TextView(this).apply {
            text = "UV Profiles"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "Exynos 990 - ExtremeROM"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(MUTED)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(20))
        })

        // --- pulsanti profilo, due per riga ---
        profiles.chunked(2).forEach { pair ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(4), 0, dp(4))
            }
            pair.forEach { (profile, label) ->
                val b = Button(this).apply {
                    text = label
                    textSize = 13f
                    setPadding(dp(8), dp(12), dp(8), dp(12))
                    setOnClickListener { applyProfile(profile) }
                }
                row.addView(b, LinearLayout.LayoutParams(0, dpWrap(), 1f).apply {
                    if (pair.size == 1) width = 0
                })
            }
            // colma la riga se c'e un solo bottone
            if (pair.size == 1) {
                row.addView(View(this), LinearLayout.LayoutParams(0, dpWrap(), 1f))
            }
            root.addView(row)
        }

        // --- sezione status ---
        root.addView(TextView(this).apply {
            text = "STATUS"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTextColor(ACCENT)
            gravity = Gravity.CENTER
            setPadding(0, dp(24), 0, dp(10))
        })

        chart = ClusterChartView(this)
        root.addView(chart, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(150)
        ))

        statusBlock = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(12), dp(4), 0)
        }
        root.addView(statusBlock)

        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        refresh()
        // aggiornamento periodico: il carico cambia continuamente
        handler.postDelayed(autoRefresh, REFRESH_MS)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(autoRefresh)
    }

    private val autoRefresh = object : Runnable {
        override fun run() {
            refresh()
            handler.postDelayed(this, REFRESH_MS)
        }
    }

    private fun applyProfile(profile: String) {
        // Lo script vive nella risorsa raw: va copiato su file la prima
        // volta e comunque se /data/local/tmp e stato cancellato.
        if (!RootShell.writeScript(resources, R.raw.uv_profiles, RootShell.APPLY_SCRIPT)) {
            Toast.makeText(this, "Non riesco a scrivere lo script: serve root", Toast.LENGTH_LONG).show()
            return
        }
        Toast.makeText(this, "Applico $profile...", Toast.LENGTH_SHORT).show()
        val rc = RootShell.runForCode("sh ${RootShell.APPLY_SCRIPT} $profile")
        if (rc == 0) {
            Toast.makeText(this, "Profilo $profile applicato", Toast.LENGTH_SHORT).show()
            handler.postDelayed({ refresh() }, 400)
        } else if (rc == -1) {
            Toast.makeText(this, "Serve root: autorizza UV Profiles in Magisk", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Profilo $profile non applicato", Toast.LENGTH_LONG).show()
        }
    }

    private fun refresh() {
        val d = UvReader.read(resources)
        chart.setData(d)
        renderText(d)
        if (!d.rootOk) {
            statusBlock.addView(
                TextView(this).apply {
                    text = "root non concesso: autorizza l'app in Magisk"
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    setTextColor(Color.parseColor("#FF8A80"))
                    setPadding(0, dp(8), 0, 0)
                }
            )
        }
    }

    private fun renderText(d: UvData) {
        statusBlock.removeAllViews()
        statusBlock.addView(row("profilo", d.profile))
        // Le frequenze dal kernel sono in kHz, da mostrare in MHz.
        fun mhz(khz: Int) = String.format("%.0f", khz / 1000.0)
        statusBlock.addView(row("LITTLE  cpu0-3", "${mhz(d.littleCur)} / ${mhz(d.littleMax)} MHz"))
        statusBlock.addView(row("MIDDLE  cpu4,5", "${mhz(d.middleCur)} / ${mhz(d.middleMax)} MHz"))
        statusBlock.addView(row("PRIME   cpu6,7", "${mhz(d.primeCur)} / ${mhz(d.primeMax)} MHz"))
        statusBlock.addView(row("core online", "${d.coresOnline} / 8"))
        statusBlock.addView(sep())
        statusBlock.addView(row("margin lit", d.mLit))
        statusBlock.addView(row("margin mid", d.mMid))
        statusBlock.addView(row("margin big", d.mBig))
        statusBlock.addView(row("margin g3d", d.mG3d))
        statusBlock.addView(row("margin disp", d.mDisp))
        statusBlock.addView(row("margin mif", d.mMif))
        statusBlock.addView(row("margin npu", d.mNpu))
        statusBlock.addView(sep())
        statusBlock.addView(row("carico", d.load))
        statusBlock.addView(row("temperatura", d.tempC))
        statusBlock.addView(row("memoria disp.", d.memAvail))
        statusBlock.addView(row("swap", d.swapFree))
    }

    private fun row(label: String, value: String) = TextView(this).apply {
        text = String.format("%-16s %s", label, value)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        setTextColor(if (label == "profilo") ACCENT else Color.WHITE)
        setPadding(0, dp(3), 0, dp(3))
    }

    private fun sep() = View(this).apply {
        setBackgroundColor(0x22FFFFFF)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
        ).apply { topMargin = dp(8); bottomMargin = dp(6) }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun dpWrap() = LinearLayout.LayoutParams.WRAP_CONTENT

    companion object {
        private const val REFRESH_MS = 3000L
        val BG = Color.parseColor("#12121A")
        val MUTED = Color.parseColor("#8A8AA0")
        val ACCENT = Color.parseColor("#4FC3F7")
    }
}
