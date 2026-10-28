package nl.rechtopzitten

import android.content.Context
import java.util.Calendar

/** Fixed work hours: switches the tracker on and off by itself. */
object Scheduler {

    /** 0 = Monday … 6 = Sunday */
    private fun dayIndex(c: Calendar) = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7

    private fun at(base: Calendar, minutes: Int): Long {
        val c = base.clone() as Calendar
        c.set(Calendar.HOUR_OF_DAY, minutes / 60)
        c.set(Calendar.MINUTE, minutes % 60)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun valid(s: Store) = s.schedOn && s.startMin < s.endMin

    private fun dayOffset(now: Long, days: Int): Calendar =
        Calendar.getInstance().apply { timeInMillis = now; add(Calendar.DAY_OF_YEAR, days) }

    /** [start, end] of the work window that contains `now`, or null. */
    fun windowAt(s: Store, now: Long): LongArray? {
        if (!valid(s)) return null
        val c = dayOffset(now, 0)
        if (s.days[dayIndex(c)] != '1') return null
        val a = at(c, s.startMin)
        val b = at(c, s.endMin)
        return if (now in a until b) longArrayOf(a, b) else null
    }

    fun nextTransition(s: Store, now: Long): Long? {
        if (!valid(s)) return null
        for (d in 0..7) {
            val c = dayOffset(now, d)
            if (s.days[dayIndex(c)] != '1') continue
            val a = at(c, s.startMin)
            val b = at(c, s.endMin)
            if (a > now) return a
            if (b > now) return b
        }
        return null
    }

    fun nextStart(s: Store, now: Long): Long? {
        if (!valid(s)) return null
        for (d in 0..7) {
            val c = dayOffset(now, d)
            if (s.days[dayIndex(c)] != '1') continue
            val a = at(c, s.startMin)
            if (a > now) return a
        }
        return null
    }

    /** Bring the tracker in line with the schedule and set the alarm for the next switch. */
    fun apply(ctx: Context) {
        val s = Store(ctx)
        val now = System.currentTimeMillis()
        Alarms.cancelTransition(ctx)
        if (!valid(s)) return
        if (windowAt(s, now) != null) {
            if (s.mode == Store.MODE_OFF) {
                if (now >= s.suppressUntil) Tracker.start(ctx, bySchedule = true)
            } else {
                s.bySchedule = true
            }
        } else if (s.mode != Store.MODE_OFF && s.bySchedule) {
            Tracker.stop(ctx, manual = false)
        }
        nextTransition(s, now)?.let { Alarms.scheduleTransition(ctx, it + 1_000) }
    }
}
