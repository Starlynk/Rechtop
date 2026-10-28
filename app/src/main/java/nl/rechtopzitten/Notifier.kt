package nl.rechtopzitten

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.TileService

object Notifier {
    private const val CH_NUDGE = "nudge"
    private const val CH_STATUS = "status"
    private const val ID_NUDGE = 1
    private const val ID_STATUS = 2

    private fun nm(ctx: Context) = ctx.getSystemService(NotificationManager::class.java)

    fun channels(ctx: Context) {
        nm(ctx).createNotificationChannel(
            NotificationChannel(CH_NUDGE, "Houdingseintjes", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Het seintje om je houding te checken"
                enableVibration(true)
            }
        )
        nm(ctx).createNotificationChannel(
            NotificationChannel(CH_STATUS, "Status", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Laat zien of je aan je bureau zit of even weg bent"
                setShowBadge(false)
            }
        )
    }

    fun canPost(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun openApp(ctx: Context): PendingIntent =
        PendingIntent.getActivity(
            ctx, 0,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun action(ctx: Context, act: String, label: String, req: Int): Notification.Action =
        Notification.Action.Builder(
            Icon.createWithResource(ctx, R.drawable.ic_spine),
            label,
            Alarms.broadcast(ctx, act, req)
        ).build()

    fun nudge(ctx: Context, tip: String) {
        channels(ctx)
        if (!canPost(ctx)) return
        val n = Notification.Builder(ctx, CH_NUDGE)
            .setSmallIcon(R.drawable.ic_spine)
            .setColor(ctx.getColor(R.color.accent))
            .setContentTitle("Hoe zit je nu?")
            .setContentText(tip)
            .setStyle(Notification.BigTextStyle().bigText(tip))
            .setCategory(Notification.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openApp(ctx))
            .addAction(action(ctx, Alarms.ACTION_ACK, "Zit goed", 10))
            .addAction(action(ctx, Alarms.ACTION_AWAY, "Even weg", 11))
            .build()
        nm(ctx).notify(ID_NUDGE, n)
    }

    fun cancelNudge(ctx: Context) = nm(ctx).cancel(ID_NUDGE)

    /** The quiet, ongoing notification with a live timer and the Away/Back button. */
    fun status(ctx: Context) {
        refreshTile(ctx)
        channels(ctx)
        val s = Store(ctx)
        if (s.mode == Store.MODE_OFF || !canPost(ctx)) {
            nm(ctx).cancel(ID_STATUS)
            return
        }
        val away = s.mode == Store.MODE_AWAY
        val b = Notification.Builder(ctx, CH_STATUS)
            .setSmallIcon(R.drawable.ic_spine)
            .setColor(ctx.getColor(R.color.accent))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setContentIntent(openApp(ctx))
        if (away) {
            b.setWhen(s.awaySince)
                .setContentTitle("Even weg")
                .setContentText("Seintjes staan op pauze")
                .addAction(action(ctx, Alarms.ACTION_BACK, "Ik ben terug", 12))
        } else {
            b.setWhen(s.sitStart)
                .setContentTitle("Aan je bureau")
                .setContentText("De tijd laat zien hoe lang je al aaneen zit")
                .addAction(action(ctx, Alarms.ACTION_AWAY, "Even weg", 11))
        }
        nm(ctx).notify(ID_STATUS, b.build())
    }

    fun clearAll(ctx: Context) {
        nm(ctx).cancel(ID_NUDGE)
        nm(ctx).cancel(ID_STATUS)
        refreshTile(ctx)
    }

    private fun refreshTile(ctx: Context) {
        try {
            TileService.requestListeningState(ctx, ComponentName(ctx, AwayTile::class.java))
        } catch (e: Exception) {
            // Tile not added; nothing to refresh.
        }
    }
}
