package nl.rechtopzitten

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

/** State changes: start, stop, away, back, nudges. Every change goes through here. */
object Tracker {
    const val MIN = 60_000L
    const val BREAK_MS = 2 * MIN

    val tips = listOf(
        "Voeten plat op de grond, knieën op heuphoogte.",
        "Schouders omlaag en iets naar achteren.",
        "Kin licht intrekken, hoofd boven je schouders.",
        "Onderrug tegen de rugleuning.",
        "Scherm op ooghoogte, armen ontspannen.",
        "Even rekken: armen omhoog, diep inademen.",
        "Ontspan je kaak en je schouders."
    )

    fun today(now: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))

    private fun rollover(s: Store, now: Long) {
        val key = today(now)
        if (s.dayKey != key) {
            s.dayKey = key
            s.deskMs = 0
            s.breakMs = 0
            s.breaks = 0
            s.log = "[]"
            if (s.mode == Store.MODE_DESK) s.segStart = now
        }
    }

    private fun closeSegment(s: Store, now: Long) {
        if (s.mode == Store.MODE_DESK) {
            s.deskMs = s.deskMs + max(0L, now - s.segStart)
            s.segStart = now
        }
    }

    fun start(ctx: Context, bySchedule: Boolean) {
        val s = Store(ctx)
        val now = System.currentTimeMillis()
        rollover(s, now)
        val inWindow = Scheduler.windowAt(s, now) != null
        if (s.mode != Store.MODE_OFF) {
            if (bySchedule) s.bySchedule = true
            return
        }
        s.mode = Store.MODE_DESK
        s.segStart = now
        s.sitStart = now
        s.pendingSince = 0
        s.bySchedule = bySchedule || inWindow
        Alarms.scheduleNudge(ctx)
        Notifier.status(ctx)
    }

    fun stop(ctx: Context, manual: Boolean) {
        val s = Store(ctx)
        val now = System.currentTimeMillis()
        rollover(s, now)
        if (s.mode == Store.MODE_OFF) return
        if (s.mode == Store.MODE_AWAY) finishBreak(s, now)
        closeSegment(s, now)
        s.mode = Store.MODE_OFF
        s.pendingSince = 0
        s.bySchedule = false
        // A manual stop inside the work window keeps it off until the window ends.
        s.suppressUntil = if (manual) (Scheduler.windowAt(s, now)?.get(1) ?: 0L) else 0L
        Alarms.cancelNudge(ctx)
        Notifier.clearAll(ctx)
    }

    fun goAway(ctx: Context) {
        val s = Store(ctx)
        val now = System.currentTimeMillis()
        rollover(s, now)
        if (s.mode != Store.MODE_DESK) return
        closeSegment(s, now)
        s.pendingSince = 0
        s.mode = Store.MODE_AWAY
        s.awaySince = now
        Alarms.cancelNudge(ctx)
        Notifier.cancelNudge(ctx)
        Notifier.status(ctx)
    }

    /** Returns how long the person was away, in ms (or -1 if they weren't away). */
    fun comeBack(ctx: Context): Long {
        val s = Store(ctx)
        val now = System.currentTimeMillis()
        rollover(s, now)
        if (s.mode != Store.MODE_AWAY) return -1
        val ms = finishBreak(s, now)
        s.mode = Store.MODE_DESK
        s.segStart = now
        Alarms.scheduleNudge(ctx)
        Notifier.status(ctx)
        return ms
    }

    private fun finishBreak(s: Store, now: Long): Long {
        val ms = max(0L, now - s.awaySince)
        if (ms >= BREAK_MS) {
            s.breaks = s.breaks + 1
            s.breakMs = s.breakMs + ms
            s.sitStart = now
            addLog(s, "break", s.awaySince, now)
        }
        return ms
    }

    fun nudgeFired(ctx: Context) {
        val s = Store(ctx)
        val now = System.currentTimeMillis()
        rollover(s, now)
        if (s.mode != Store.MODE_DESK) return
        if (s.pendingSince != 0L) addLog(s, "missed", s.pendingSince, 0)
        s.pendingSince = now
        Notifier.nudge(ctx, tips.random())
        Alarms.scheduleNudge(ctx)
    }

    fun ack(ctx: Context) {
        val s = Store(ctx)
        val now = System.currentTimeMillis()
        rollover(s, now)
        if (s.pendingSince != 0L) {
            addLog(s, "posture", s.pendingSince, now - s.pendingSince)
            s.pendingSince = 0
        }
        Notifier.cancelNudge(ctx)
    }

    /** After a reboot or app update the alarms are gone; put them back. */
    fun restore(ctx: Context) {
        val s = Store(ctx)
        when (s.mode) {
            Store.MODE_DESK -> { Alarms.scheduleNudge(ctx); Notifier.status(ctx) }
            Store.MODE_AWAY -> Notifier.status(ctx)
        }
    }

    private fun addLog(s: Store, kind: String, t: Long, extra: Long) {
        val old = try { JSONArray(s.log) } catch (e: Exception) { JSONArray() }
        val out = JSONArray()
        out.put(JSONObject().put("k", kind).put("t", t).put("x", extra))
        for (i in 0 until minOf(old.length(), 59)) out.put(old.get(i))
        s.log = out.toString()
    }

    data class Snapshot(val deskMs: Long, val sitMs: Long, val breaks: Int, val breakMs: Long)

    fun snapshot(s: Store, now: Long): Snapshot {
        if (s.dayKey != today(now)) {
            val sit = if (s.mode == Store.MODE_DESK) max(0L, now - s.sitStart) else 0L
            return Snapshot(if (s.mode == Store.MODE_DESK) max(0L, now - s.segStart) else 0L, sit, 0, 0)
        }
        val desk = s.deskMs + if (s.mode == Store.MODE_DESK) max(0L, now - s.segStart) else 0L
        val sit = if (s.mode == Store.MODE_DESK) max(0L, now - s.sitStart) else 0L
        return Snapshot(desk, sit, s.breaks, s.breakMs)
    }

    fun dur(ms: Long): String {
        val m = ms / MIN
        val h = m / 60
        return if (h > 0) "${h}u ${(m % 60).toString().padStart(2, '0')}m" else "${m}m"
    }
}
