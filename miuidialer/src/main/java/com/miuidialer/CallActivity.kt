package com.miuidialer

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.VideoProfile
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

/** Full-screen MIUI-style incoming / outgoing / active call screen. */
class CallActivity : Activity() {

    private lateinit var avatar: TextView
    private lateinit var nameTv: TextView
    private lateinit var numTv: TextView
    private lateinit var statusTv: TextView
    private lateinit var actions: LinearLayout

    private val handler = Handler(Looper.getMainLooper())
    private var activeSince = 0L
    private var muted = false
    private var speaker = false

    private val ticker = object : Runnable {
        override fun run() {
            updateTimer()
            handler.postDelayed(this, 1000)
        }
    }

    private val refresh: () -> Unit = { runOnUiThread { render() } }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        darkBars()
        build()
        CallBus.add(refresh)
    }

    override fun onDestroy() {
        CallBus.remove(refresh)
        handler.removeCallbacks(ticker)
        super.onDestroy()
    }

    private fun build() {
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER_HORIZONTAL
        root.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(M.DARK2, M.DARK)
        )
        root.setPadding(dp(24), dp(56), dp(24), dp(40))

        statusTv = tv("", 14f, 0x99FFFFFF.toInt())
        statusTv.gravity = Gravity.CENTER
        root.addView(statusTv, lp(MATCH, WRAP))

        avatar = tv("#", 40f, M.WHITE, true)
        avatar.gravity = Gravity.CENTER
        avatar.background = circle(0x33FFFFFF)
        root.addView(avatar, lp(dp(112), dp(112), 0f, 0, dp(36), 0, 0))

        nameTv = tv("", 28f, M.WHITE, true)
        nameTv.gravity = Gravity.CENTER
        nameTv.maxLines = 1
        root.addView(nameTv, lp(MATCH, WRAP, 0f, 0, dp(24), 0, 0))

        numTv = tv("", 16f, 0x99FFFFFF.toInt())
        numTv.gravity = Gravity.CENTER
        root.addView(numTv, lp(MATCH, WRAP, 0f, 0, dp(6), 0, 0))

        root.addView(View(this), lp(0, 0, 1f))

        actions = LinearLayout(this)
        actions.gravity = Gravity.CENTER
        root.addView(actions, lp(MATCH, WRAP, 0f, 0, 0, 0, dp(24)))

        setContentView(root)
        render()
    }

    private fun render() {
        val call = CallBus.call
        if (call == null) { finish(); return }

        val state = call.state
        val number = call.details?.handle?.schemeSpecificPart.orEmpty()
        val name = Phonebook.nameFor(this, number)

        nameTv.text = name ?: if (number.isBlank()) "Unknown" else number
        numTv.text = if (name != null) number else ""
        avatar.text = Phonebook.initial(name ?: number)

        actions.removeAllViews()

        when (state) {
            Call.STATE_RINGING -> {
                statusTv.text = "Incoming call"
                handler.removeCallbacks(ticker)
                actions.addView(
                    callAction(R.drawable.ic_call_end, "Decline", M.RED) {
                        runCatching { call.reject(false, null) }
                        finish()
                    },
                    lp(0, WRAP, 1f)
                )
                actions.addView(
                    callAction(R.drawable.ic_phone, "Answer", M.GREEN) {
                        runCatching { call.answer(VideoProfile.STATE_AUDIO_ONLY) }
                    },
                    lp(0, WRAP, 1f)
                )
            }

            Call.STATE_DIALING, Call.STATE_CONNECTING, Call.STATE_NEW -> {
                statusTv.text = "Calling\u2026"
                handler.removeCallbacks(ticker)
                actions.addView(
                    callAction(R.drawable.ic_call_end, "End call", M.RED) {
                        runCatching { call.disconnect() }
                        finish()
                    },
                    lp(0, WRAP, 1f)
                )
            }

            Call.STATE_ACTIVE -> {
                if (activeSince == 0L) {
                    activeSince = System.currentTimeMillis()
                    handler.post(ticker)
                }
                updateTimer()
                actions.addView(
                    callAction(R.drawable.ic_mic_off, if (muted) "Unmute" else "Mute", M.GLASS) {
                        muted = !muted
                        CallBus.service?.setMuted(muted)
                        render()
                    },
                    lp(0, WRAP, 1f)
                )
                actions.addView(
                    callAction(R.drawable.ic_keypad, "Keypad", M.GLASS) { },
                    lp(0, WRAP, 1f)
                )
                actions.addView(
                    callAction(R.drawable.ic_speaker, if (speaker) "Handset" else "Speaker", M.GLASS) {
                        speaker = !speaker
                        CallBus.service?.setAudioRoute(
                            if (speaker) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE
                        )
                        render()
                    },
                    lp(0, WRAP, 1f)
                )
                actions.addView(
                    callAction(R.drawable.ic_call_end, "End call", M.RED) {
                        runCatching { call.disconnect() }
                        finish()
                    },
                    lp(0, WRAP, 1f)
                )
            }

            Call.STATE_HOLDING -> {
                statusTv.text = "On hold"
                actions.addView(
                    callAction(R.drawable.ic_phone, "Resume", M.GREEN) {
                        runCatching { call.unhold() }
                    },
                    lp(0, WRAP, 1f)
                )
                actions.addView(
                    callAction(R.drawable.ic_call_end, "End call", M.RED) {
                        runCatching { call.disconnect() }
                        finish()
                    },
                    lp(0, WRAP, 1f)
                )
            }

            Call.STATE_DISCONNECTED -> {
                handler.removeCallbacks(ticker)
                finish()
            }

            else -> {
                statusTv.text = "Connecting\u2026"
            }
        }
    }

    private fun updateTimer() {
        if (activeSince == 0L) return
        val secs = (System.currentTimeMillis() - activeSince) / 1000
        statusTv.text = String.format("%02d:%02d", secs / 60, secs % 60)
    }
}
