package it.davide.uvprofiles

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/**
 * Widget home screen: 6 tasti.
 * ECO | BAL | SIXCORE | SPORT | OFF | STATUS
 */
class UvWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) {
            appWidgetManager.updateAppWidget(id, buildViews(context, id))
        }
    }

    private fun buildViews(context: Context, id: Int): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_uv).apply {
            setOnClickPendingIntent(R.id.btn_eco, action(context, "eco", id))
            setOnClickPendingIntent(R.id.btn_balanced, action(context, "balanced", id))
            setOnClickPendingIntent(R.id.btn_sixcore, action(context, "sixcore", id))
            setOnClickPendingIntent(R.id.btn_sport, action(context, "sport", id))
            setOnClickPendingIntent(R.id.btn_off, action(context, "off", id))
            setOnClickPendingIntent(R.id.btn_status, action(context, "status", id))
        }

    private fun action(context: Context, profile: String, id: Int): PendingIntent {
        val intent = Intent(context, UvWidgetReceiver::class.java).apply {
            action = ACTION_APPLY
            putExtra(EXTRA_PROFILE, profile)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        }
        return PendingIntent.getBroadcast(
            context,
            (profile + id).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_APPLY = "it.davide.uvprofiles.APPLY"
        const val EXTRA_PROFILE = "profile"
    }
}

/**
 * Esegue il profilo richiesto e aggiorna il widget con l'esito.
 * `su` gira in un thread separato: non puo essere eseguito direttamente
 * dentro onReceive, perche il binder va in timeout.
 */
class UvWidgetReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != UvWidgetProvider.ACTION_APPLY) return
        val profile = intent.getStringExtra(UvWidgetProvider.EXTRA_PROFILE) ?: return
        val id = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )

        Thread {
            // Lo script di applicazione e una risorsa raw dell'app: il
            // widget non dipende da alcun modulo Magisk.
            RootShell.writeScript(context.resources, R.raw.uv_profiles, RootShell.APPLY_SCRIPT)
            val rc = RootShell.runForCode("sh ${RootShell.APPLY_SCRIPT} $profile")

            val label = if (rc == 0) profile else "errore"
            val views = RemoteViews(context.packageName, R.layout.widget_uv).apply {
                setTextViewText(R.id.status, label)
                setOnClickPendingIntent(R.id.btn_eco, pending(context, "eco", id))
                setOnClickPendingIntent(R.id.btn_balanced, pending(context, "balanced", id))
                setOnClickPendingIntent(R.id.btn_sixcore, pending(context, "sixcore", id))
                setOnClickPendingIntent(R.id.btn_sport, pending(context, "sport", id))
                setOnClickPendingIntent(R.id.btn_off, pending(context, "off", id))
                setOnClickPendingIntent(R.id.btn_status, pending(context, "status", id))
            }
            AppWidgetManager.getInstance(context).updateAppWidget(id, views)
        }.start()
    }

    private fun pending(context: Context, profile: String, id: Int): PendingIntent {
        val intent = Intent(context, UvWidgetReceiver::class.java).apply {
            action = UvWidgetProvider.ACTION_APPLY
            putExtra(UvWidgetProvider.EXTRA_PROFILE, profile)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        }
        return PendingIntent.getBroadcast(
            context,
            (profile + id).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
