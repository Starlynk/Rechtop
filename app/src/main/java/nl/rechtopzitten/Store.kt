package nl.rechtopzitten

import android.content.Context
import android.content.SharedPreferences

/** All state lives in SharedPreferences and is read live, so receivers, tile and UI always agree. */
class Store(ctx: Context) {
    private val p: SharedPreferences =
        ctx.applicationContext.getSharedPreferences("rechtop", Context.MODE_PRIVATE)

    private fun long(key: String) = p.getLong(key, 0L)
    private fun setLong(key: String, v: Long) { p.edit().putLong(key, v).commit() }

    // Settings
    var minMin: Int
        get() = p.getInt("minMin", 15)
        set(v) { p.edit().putInt("minMin", v).commit() }
    var maxMin: Int
        get() = p.getInt("maxMin", 45)
        set(v) { p.edit().putInt("maxMin", v).commit() }
    var schedOn: Boolean
        get() = p.getBoolean("schedOn", false)
        set(v) { p.edit().putBoolean("schedOn", v).commit() }
    /** Seven characters, Monday first: '1' = on. */
    var days: String
        get() = (p.getString("days", "1111100") ?: "1111100").padEnd(7, '0').take(7)
        set(v) { p.edit().putString("days", v).commit() }
    /** Minutes after midnight. */
    var startMin: Int
        get() = p.getInt("startMin", 9 * 60)
        set(v) { p.edit().putInt("startMin", v).commit() }
    var endMin: Int
        get() = p.getInt("endMin", 17 * 60 + 30)
        set(v) { p.edit().putInt("endMin", v).commit() }

    // Runtime state: "off", "desk" or "away"
    var mode: String
        get() = p.getString("mode", MODE_OFF) ?: MODE_OFF
        set(v) { p.edit().putString("mode", v).commit() }
    var bySchedule: Boolean
        get() = p.getBoolean("bySchedule", false)
        set(v) { p.edit().putBoolean("bySchedule", v).commit() }
    var awaySince: Long
        get() = long("awaySince")
        set(v) { setLong("awaySince", v) }
    var segStart: Long
        get() = long("segStart")
        set(v) { setLong("segStart", v) }
    var sitStart: Long
        get() = long("sitStart")
        set(v) { setLong("sitStart", v) }
    var pendingSince: Long
        get() = long("pendingSince")
        set(v) { setLong("pendingSince", v) }
    var suppressUntil: Long
        get() = long("suppressUntil")
        set(v) { setLong("suppressUntil", v) }

    // Today's totals
    var dayKey: String
        get() = p.getString("dayKey", "") ?: ""
        set(v) { p.edit().putString("dayKey", v).commit() }
    var deskMs: Long
        get() = long("deskMs")
        set(v) { setLong("deskMs", v) }
    var breakMs: Long
        get() = long("breakMs")
        set(v) { setLong("breakMs", v) }
    var breaks: Int
        get() = p.getInt("breaks", 0)
        set(v) { p.edit().putInt("breaks", v).commit() }
    var log: String
        get() = p.getString("log", "[]") ?: "[]"
        set(v) { p.edit().putString("log", v).commit() }

    companion object {
        const val MODE_OFF = "off"
        const val MODE_DESK = "desk"
        const val MODE_AWAY = "away"
    }
}
