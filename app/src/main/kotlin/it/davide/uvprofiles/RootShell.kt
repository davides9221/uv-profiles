package it.davide.uvprofiles

import android.content.res.Resources

/**
 * Accesso a root tramite `su`.
 *
 * Gli script vivono come risorse raw e vengono copiati su un file a ogni
 * uso, poi eseguiti come `su -c "sh <file>"`.
 *
 * Perche non passare lo script come argomento di `su -c`:
 *
 *  - In Kotlin `$` e il placeholder di interpolazione. Lo script e pieno di
 *    variabili di shell, e ogni `$` verrebbe risolto da Kotlin invece che
 *    dalla shell, mandando vuote le variabili. Con percorsi tipo
 *    /sys/power/percent_margin (permessi 600 root) la lettura fallisce in
 *    silenzio, perche l'errore e rediretto.
 *  - Lo script inline era multilinea, e alcune implementazioni di su lo
 *    troncano a meta.
 *
 * Tenendo lo script in res/raw non si risolve nessuno dei due: i byte
 * arrivano al file esattamente come sono stati scritti.
 */
object RootShell {

    const val READ_SCRIPT = "/data/local/tmp/uv_profiles_read.sh"

    /**
     * Percorso persistente: /data/local/tmp viene cancellato a ogni reboot,
     * e serve che lo script di applicazione sopravviva, perche e il boot
     * script in /data/adb/service.d a richiamarlo.
     */
    const val APPLY_SCRIPT = "/data/adb/uv-profiles/apply.sh"

    /** Esegue un comando come root e restituisce l'output. Stringa vuota in caso di errore. */
    fun run(cmd: String): String = try {
        Runtime.getRuntime()
            .exec(arrayOf("su", "-c", cmd))
            .inputStream.bufferedReader()
            .readText()
    } catch (e: Exception) {
        ""
    }

    /** Esegue un comando come root e restituisce il codice di uscita. -1 se non e partito. */
    fun runForCode(cmd: String): Int = try {
        val p = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
        p.inputStream.close(); p.outputStream.close(); p.errorStream.close()
        p.waitFor()
    } catch (e: Exception) {
        -1
    }

    /**
     * Copia una risorsa raw su un file eseguibile da root.
     * La cartella viene creata se manca: /data/local/tmp non e scrivibile
     * dall'app senza root, quindi il passaggio avviene mandando lo script
     * su stdin di `cat`.
     */
    fun writeScript(res: Resources, resId: Int, path: String): Boolean = try {
        val dir = path.substringBeforeLast('/')
        if (dir.isNotEmpty()) runForCode("mkdir -p $dir")
        val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "cat > $path"))
        res.openRawResource(resId).use { input ->
            p.outputStream.use { out -> input.copyTo(out) }
        }
        p.waitFor()
        runForCode("chmod 755 $path") == 0
    } catch (e: Exception) {
        false
    }

    /** true se `su` e disponibile e concesso all'app. */
    fun hasRoot(): Boolean = run("id -u").trim() == "0"
}