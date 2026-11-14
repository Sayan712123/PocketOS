package com.pocketos

import android.app.Activity
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.AlarmClock
import android.text.InputType
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClockActivity : Activity() {
    private val h = Handler(Looper.getMainLooper())
    private lateinit var time: TextView
    private lateinit var sw: TextView
    private lateinit var tm: TextView
    private lateinit var swBtn: TextView
    private lateinit var mins: EditText
    private var swRun = false
    private var swStart = 0L
    private var swAcc = 0L
    private var tmEnd = 0L
    private val tick = object : Runnable {
        override fun run() { update(); h.postDelayed(this, 100) }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        bars(true)
        val sv = ScrollView(this)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(16), dp(24), dp(16), dp(32))
        sv.addView(root)

        time = tv("", 56f, C.TEXT, true)
        time.gravity = Gravity.CENTER
        root.addView(time, lp(MATCH, WRAP))
        val date = tv(SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()), 16f, C.SUB)
        date.gravity = Gravity.CENTER
        root.addView(date, lp(MATCH, WRAP, 0f, 0, 0, 0, dp(20)))

        val c1 = card()
        c1.addView(tv("Stopwatch", 13f, C.SUB, true))
        sw = tv("00:00.0", 40f, C.TEXT)
        sw.gravity = Gravity.CENTER
        c1.addView(sw, lp(MATCH, WRAP, 0f, 0, dp(8), 0, dp(8)))
        val row = LinearLayout(this)
        swBtn = button("Start", C.GREEN) { toggleSw() }
        row.addView(swBtn, lp(0, WRAP, 1f, 0, 0, dp(6), 0))
        row.addView(button("Reset", C.SUB) { swRun = false; swAcc = 0; swBtn.text = "Start" }, lp(0, WRAP, 1f, dp(6), 0, 0, 0))
        c1.addView(row)
        root.addView(c1, lp(MATCH, WRAP, 0f, 0, 0, 0, dp(12)))

        val c2 = card()
        c2.addView(tv("Timer (keep app open)", 13f, C.SUB, true))
        tm = tv("00:00", 40f, C.TEXT)
        tm.gravity = Gravity.CENTER
        c2.addView(tm, lp(MATCH, WRAP, 0f, 0, dp(8), 0, 0))
        mins = EditText(this)
        mins.hint = "Minutes"
        mins.inputType = InputType.TYPE_CLASS_NUMBER
        mins.gravity = Gravity.CENTER
        c2.addView(mins)
        c2.addView(button("Start timer") {
            val m = mins.text.toString().toLongOrNull()
            if (m != null && m > 0) tmEnd = SystemClock.elapsedRealtime() + m * 60000
        }, lp(MATCH, WRAP, 0f, 0, dp(8), 0, 0))
        root.addView(c2, lp(MATCH, WRAP, 0f, 0, 0, 0, dp(12)))

        root.addView(button("Alarms", C.ORANGE) {
            try { startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS)) } catch (e: Exception) { toast("No alarm app found") }
        })
        setContentView(sv)
    }

    private fun toggleSw() {
        val now = SystemClock.elapsedRealtime()
        if (swRun) { swAcc += now - swStart; swRun = false; swBtn.text = "Start" }
        else { swStart = now; swRun = true; swBtn.text = "Stop" }
    }

    private fun update() {
        time.text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val now = SystemClock.elapsedRealtime()
        val e = swAcc + (if (swRun) now - swStart else 0L)
        sw.text = String.format(Locale.US, "%02d:%02d.%d", e / 60000, (e / 1000) % 60, (e / 100) % 10)
        if (tmEnd > 0) {
            val r = tmEnd - now
            if (r <= 0) {
                tmEnd = 0
                tm.text = "00:00"
                try { ToneGenerator(AudioManager.STREAM_ALARM, 100).startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 2000) } catch (e: Exception) { }
            } else {
                val s = (r + 999) / 1000
                tm.text = String.format(Locale.US, "%02d:%02d", s / 60, s % 60)
            }
        }
    }

    override fun onResume() { super.onResume(); h.post(tick) }
    override fun onPause() { h.removeCallbacks(tick); super.onPause() }
}
