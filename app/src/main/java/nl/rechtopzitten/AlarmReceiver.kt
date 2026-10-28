package nl.rechtopzitten

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.action) {
            Alarms.ACTION_NUDGE -> Tracker.nudgeFired(ctx)
            Alarms.ACTION_SCHEDULE -> Scheduler.apply(ctx)
            Alarms.ACTION_ACK -> Tracker.ack(ctx)
            Alarms.ACTION_AWAY -> Tracker.goAway(ctx)
            Alarms.ACTION_BACK -> Tracker.comeBack(ctx)
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Tracker.restore(ctx)
                Scheduler.apply(ctx)
            }
        }
    }
}
