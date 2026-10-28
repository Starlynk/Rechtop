package nl.rechtopzitten

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import kotlin.random.Random

/**
 * Inexact alarms that still fire in Doze. Random reminders don't need to be
 * exact, so the app needs no special alarm permission.
 */
object Alarms {
    const val ACTION_NUDGE = "nl.rechtopzitten.NUDGE"
    const val ACTION_SCHEDULE = "nl.rechtopzitten.SCHEDULE"
    const val ACTION_ACK = "nl.rechtopzitten.ACK"
    const val ACTION_AWAY = "nl.rechtopzitten.AWAY"
    const val ACTION_BACK = "nl.rechtopzitten.BACK"

    private const val REQ_NUDGE = 1
    private const val REQ_SCHEDULE = 2

    fun broadcast(ctx: Context, action: String, req: Int): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, req,
            Intent(ctx, AlarmReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun am(ctx: Context) = ctx.getSystemService(AlarmManager::class.java)

    fun scheduleNudge(ctx: Context) {
        val s = Store(ctx)
        val mn = s.minMin.toDouble()
        val mx = maxOf(s.maxMin.toDouble(), mn + 1)
        val delay = ((mn + Random.nextDouble() * (mx - mn)) * Tracker.MIN).toLong()
        am(ctx).setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + delay,
            broadcast(ctx, ACTION_NUDGE, REQ_NUDGE)
        )
    }

    fun cancelNudge(ctx: Context) = am(ctx).cancel(broadcast(ctx, ACTION_NUDGE, REQ_NUDGE))

    fun scheduleTransition(ctx: Context, at: Long) {
        am(ctx).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, broadcast(ctx, ACTION_SCHEDULE, REQ_SCHEDULE))
    }

    fun cancelTransition(ctx: Context) = am(ctx).cancel(broadcast(ctx, ACTION_SCHEDULE, REQ_SCHEDULE))
}
