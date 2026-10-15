package it.davide.uvprofiles

import android.content.res.Resources

/** Dati raccolti dal kernel tramite `su`, per il pannello status. */
data class UvData(
    val profile: String = "?",
    val littleCur: Int = 0, val littleMax: Int = 0, val littleHw: Int = 1,
    val middleCur: Int = 0, val middleMax: Int = 0, val middleHw: Int = 1,
    val primeCur: Int = 0, val primeMax: Int = 0, val primeHw: Int = 1,
    val coresOnline: Int = 0,
    val mLit: String = "?", val mMid: String = "?", val mBig: String = "?",
    val mG3d: String = "?", val mDisp: String = "?", val mMif: String = "?",
    val mNpu: String = "?",
    val load: String = "?",
    val tempC: String = "?",
    val memAvail: String = "?",
    val swapFree: String = "?",
    val rootOk: Boolean = false
)

/**
 * Legge i valori reali dal kernel.
 *
 * Lo script di lettura e una risorsa raw (res/raw/uv_profiles_read.sh):
 * non passa mai per Kotlin, quindi i `$` arrivano alla shell intatti.
 * Vedi RootShell per il motivo.
 */
object UvReader {

    private var scriptReady = false

    fun read(res: Resources): UvData = try {
        if (!scriptReady) {
            if (RootShell.writeScript(res, R.raw.uv_profiles_read, RootShell.READ_SCRIPT)) {
                scriptReady = true
            }
        }
        val raw = RootShell.run("sh ${RootShell.READ_SCRIPT}")
        // Se il file e sparito o corrotto la lettura torna vuota: riscrivo
        // e riprovo una volta.
        if (raw.contains("CORES@")) {
            parse(raw, rootOk = true)
        } else {
            RootShell.writeScript(res, R.raw.uv_profiles_read, RootShell.READ_SCRIPT)
            parse(RootShell.run("sh ${RootShell.READ_SCRIPT}"), rootOk = true)
        }
    } catch (e: Exception) {
        UvData(profile = "errore", rootOk = false)
    }

    private fun parse(raw: String, rootOk: Boolean): UvData {
        val m = HashMap<String, String>()
        for (line in raw.lines()) {
            val t = line.trim()
            if (t.isEmpty()) continue
            val i = t.indexOf('@')
            if (i > 0) m[t.substring(0, i)] = t.substring(i + 1)
        }
        val l = m["LIT"]?.split("@") ?: listOf("0", "0", "1")
        val mi = m["MID"]?.split("@") ?: listOf("0", "0", "1")
        val p = m["PRI"]?.split("@") ?: listOf("0", "0", "1")

        fun gb(key: String): String {
            val v = m[key]?.toLongOrNull() ?: return "?"
            return String.format("%.1f GB", v / 1048576.0)
        }

        return UvData(
            profile = m["PROFILE"]?.takeIf { it.isNotBlank() } ?: "?",
            littleCur = l.getOrNull(0)?.toIntOrNull() ?: 0,
            littleMax = l.getOrNull(1)?.toIntOrNull() ?: 0,
            littleHw = l.getOrNull(2)?.toIntOrNull() ?: 1,
            middleCur = mi.getOrNull(0)?.toIntOrNull() ?: 0,
            middleMax = mi.getOrNull(1)?.toIntOrNull() ?: 0,
            middleHw = mi.getOrNull(2)?.toIntOrNull() ?: 1,
            primeCur = p.getOrNull(0)?.toIntOrNull() ?: 0,
            primeMax = p.getOrNull(1)?.toIntOrNull() ?: 0,
            primeHw = p.getOrNull(2)?.toIntOrNull() ?: 1,
            coresOnline = m["CORES"]?.toIntOrNull() ?: 0,
            mLit = m["MLIT"]?.takeIf { it.isNotBlank() } ?: "?",
            mMid = m["MMID"]?.takeIf { it.isNotBlank() } ?: "?",
            mBig = m["MBIG"]?.takeIf { it.isNotBlank() } ?: "?",
            mG3d = m["MG3D"]?.takeIf { it.isNotBlank() } ?: "?",
            mDisp = m["MDISP"]?.takeIf { it.isNotBlank() } ?: "?",
            mMif = m["MMIF"]?.takeIf { it.isNotBlank() } ?: "?",
            mNpu = m["MNPU"]?.takeIf { it.isNotBlank() } ?: "?",
            load = m["LOAD"] ?: "?",
            tempC = m["TEMP"]?.let { "$it C" } ?: "?",
            memAvail = gb("MEM"),
            swapFree = gb("SWAP"),
            rootOk = rootOk
        )
    }
}