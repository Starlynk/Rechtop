package nl.rechtopzitten

import android.Manifest
import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var s: Store
    private val handler = Handler(Looper.getMainLooper())
    private val refresher = object : Runnable {
        override fun run() {
            render()
            handler.postDelayed(this, 15_000)
        }
    }
    private val nl = Locale("nl", "NL")
    private val hm = SimpleDateFormat("HH:mm", nl)
    private val dayHm = SimpleDateFormat("EEEE HH:mm", nl)
    private val dayLabels = listOf("ma", "di", "wo", "do", "vr", "za", "zo")

    private lateinit var tvState: TextView
    private lateinit var tvStateSub: TextView
    private lateinit var btnToggle: Button
    private lateinit var btnAway: Button
    private lateinit var tvPerm: TextView
    private lateinit var tvDesk: TextView
    private lateinit var tvSit: TextView
    private lateinit var tvBreaks: TextView
    private lateinit var tvBreakTime: TextView
    private lateinit var tvMin: TextView
    private lateinit var tvMax: TextView
    private lateinit var sbMin: SeekBar
    private lateinit var sbMax: SeekBar
    private lateinit var swSched: Switch
    private lateinit var btnFrom: Button
    private lateinit var btnTo: Button
    private lateinit var tvLog: TextView
    private var lastMessage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        s = Store(this)
        Notifier.channels(this)

        tvState = findViewById(R.id.tvState)
        tvStateSub = findViewById(R.id.tvStateSub)
        btnToggle = findViewById(R.id.btnToggle)
        btnAway = findViewById(R.id.btnAway)
        tvPerm = findViewById(R.id.tvPerm)
        tvDesk = findViewById(R.id.tvDesk)
        tvSit = findViewById(R.id.tvSit)
        tvBreaks = findViewById(R.id.tvBreaks)
        tvBreakTime = findViewById(R.id.tvBreakTime)
        tvMin = findViewById(R.id.tvMin)
        tvMax = findViewById(R.id.tvMax)
        sbMin = findViewById(R.id.sbMin)
        sbMax = findViewById(R.id.sbMax)
        swSched = findViewById(R.id.swSched)
        btnFrom = findViewById(R.id.btnFrom)
        btnTo = findViewById(R.id.btnTo)
        tvLog = findViewById(R.id.tvLog)

        btnToggle.setOnClickListener {
            lastMessage = null
            if (s.mode == Store.MODE_OFF) {
                askNotificationPermission()
                Tracker.start(this, bySchedule = false)
            } else {
                Tracker.stop(this, manual = true)
            }
            render()
        }
        btnAway.setOnClickListener {
            when (s.mode) {
                Store.MODE_DESK -> { Tracker.goAway(this); lastMessage = null }
                Store.MODE_AWAY -> {
                    val ms = Tracker.comeBack(this)
                    lastMessage = if (ms >= Tracker.BREAK_MS)
                        "Welkom terug na ${Tracker.dur(ms)}. Je zittijd begint opnieuw."
                    else
                        "Korter dan 2 minuten weg, dus geen pauze geteld."
                }
            }
            render()
        }
        findViewById<Button>(R.id.btnTest).setOnClickListener {
            if (!Notifier.canPost(this)) askNotificationPermission()
            else Notifier.nudge(this, Tracker.tips.random())
        }
        tvPerm.setOnClickListener { askNotificationPermission() }
        findViewById<Button>(R.id.btnBattery).setOnClickListener {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }

        setupRanges()
        setupSchedule()
        askNotificationPermission()
        Scheduler.apply(this)
    }

    override fun onResume() {
        super.onResume()
        handler.post(refresher)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refresher)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Notifier.status(this)
        render()
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && !Notifier.canPost(this)) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    // ---------- settings ----------

    private fun setupRanges() {
        sbMin.progress = s.minMin - 5
        sbMax.progress = s.maxMin - 10
        labelRanges()
        sbMin.setOnSeekBarChangeListener(object : SimpleSeek() {
            override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                val mn = progress + 5
                s.minMin = mn
                if (s.maxMin < mn + 5) { s.maxMin = mn + 5; sbMax.progress = s.maxMin - 10 }
                labelRanges()
            }
        })
        sbMax.setOnSeekBarChangeListener(object : SimpleSeek() {
            override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                val mx = progress + 10
                s.maxMin = mx
                if (s.minMin > mx - 5) { s.minMin = mx - 5; sbMin.progress = s.minMin - 5 }
                labelRanges()
            }
        })
    }

    private fun labelRanges() {
        tvMin.text = "Minimaal ${s.minMin} minuten tussen seintjes"
        tvMax.text = "Maximaal ${s.maxMin} minuten tussen seintjes"
    }

    private fun setupSchedule() {
        swSched.isChecked = s.schedOn
        swSched.setOnCheckedChangeListener { _, on ->
            s.schedOn = on
            if (on) s.suppressUntil = 0
            Scheduler.apply(this)
            render()
        }

        val ll = findViewById<LinearLayout>(R.id.llDays)
        dayLabels.forEachIndexed { i, label ->
            val b = ToggleButton(this)
            b.textOn = label
            b.textOff = label
            b.isChecked = s.days[i] == '1'
            b.text = label
            b.minWidth = 0
            b.minimumWidth = 0
            b.setPadding(0, 0, 0, 0)
            b.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            b.setOnCheckedChangeListener { _, on ->
                val arr = s.days.toCharArray()
                arr[i] = if (on) '1' else '0'
                s.days = String(arr)
                Scheduler.apply(this)
                render()
            }
            ll.addView(b)
        }

        btnFrom.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                val v = h * 60 + m
                if (v >= s.endMin) toast("De begintijd moet vóór de eindtijd liggen.")
                else { s.startMin = v; Scheduler.apply(this); render() }
            }, s.startMin / 60, s.startMin % 60, true).show()
        }
        btnTo.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                val v = h * 60 + m
                if (v <= s.startMin) toast("De eindtijd moet na de begintijd liggen.")
                else { s.endMin = v; Scheduler.apply(this); render() }
            }, s.endMin / 60, s.endMin % 60, true).show()
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    private fun clock(minutes: Int) = "%02d:%02d".format(minutes / 60, minutes % 60)

    // ---------- render ----------

    private fun render() {
        val now = System.currentTimeMillis()
        val mode = s.mode

        when (mode) {
            Store.MODE_DESK -> {
                tvState.text = "Aan je bureau"
                tvState.setTextColor(getColor(R.color.accent))
                tvStateSub.text = lastMessage
                    ?: "Volgende seintje ergens tussen ${s.minMin} en ${s.maxMin} minuten."
            }
            Store.MODE_AWAY -> {
                tvState.text = "Even weg"
                tvState.setTextColor(getColor(R.color.fg))
                tvStateSub.text = "Weg sinds ${hm.format(Date(s.awaySince))} (${Tracker.dur(now - s.awaySince)}). Seintjes staan op pauze."
            }
            else -> {
                tvState.text = "Uit"
                tvState.setTextColor(getColor(R.color.muted))
                val next = Scheduler.nextStart(s, now)
                tvStateSub.text = when {
                    next != null -> "Gaat vanzelf aan op ${dayHm.format(Date(next))}. Of druk nu op Start."
                    else -> "Druk op Start als je aan je bureau gaat zitten."
                }
            }
        }

        btnToggle.text = if (mode == Store.MODE_OFF) "Start" else "Stop"
        btnAway.isEnabled = mode != Store.MODE_OFF
        btnAway.text = if (mode == Store.MODE_AWAY) "Ik ben terug" else "Even weg"
        tvPerm.visibility = if (Notifier.canPost(this)) View.GONE else View.VISIBLE

        val snap = Tracker.snapshot(s, now)
        tvDesk.text = Tracker.dur(snap.deskMs)
        tvSit.text = if (mode == Store.MODE_AWAY) "pauze" else Tracker.dur(snap.sitMs)
        tvBreaks.text = snap.breaks.toString()
        tvBreakTime.text = "samen ${Tracker.dur(snap.breakMs)}"

        btnFrom.text = "Van ${clock(s.startMin)}"
        btnTo.text = "Tot ${clock(s.endMin)}"
        val schedEnabled = s.schedOn
        btnFrom.isEnabled = schedEnabled
        btnTo.isEnabled = schedEnabled
        val ll = findViewById<LinearLayout>(R.id.llDays)
        for (i in 0 until ll.childCount) ll.getChildAt(i).isEnabled = schedEnabled

        tvLog.text = logText(now)
    }

    private fun logText(now: Long): String {
        if (s.dayKey != Tracker.today(now)) return "Nog niets vandaag."
        val arr = try { JSONArray(s.log) } catch (e: Exception) { JSONArray() }
        if (arr.length() == 0) return "Nog niets vandaag."
        val lines = mutableListOf<String>()
        for (i in 0 until minOf(arr.length(), 30)) {
            val o = arr.getJSONObject(i)
            val t = o.getLong("t")
            val x = o.optLong("x")
            val at = hm.format(Date(t))
            lines += when (o.getString("k")) {
                "posture" -> "$at  houding gecorrigeerd" +
                    if (x > Tracker.MIN) " na ${x / Tracker.MIN} min" else ""
                "missed" -> "$at  houdingseintje gemist"
                "break" -> "$at–${hm.format(Date(x))}  pauze van ${Tracker.dur(x - t)}"
                else -> continue
            }
        }
        return lines.joinToString("\n")
    }

    private abstract class SimpleSeek : SeekBar.OnSeekBarChangeListener {
        override fun onStartTrackingTouch(bar: SeekBar) {}
        override fun onStopTrackingTouch(bar: SeekBar) {}
    }
}
