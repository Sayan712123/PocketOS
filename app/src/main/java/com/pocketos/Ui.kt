package com.pocketos

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

object C {
    val BG = 0xFFF2F2F7.toInt()
    val CARD = 0xFFFFFFFF.toInt()
    val TEXT = 0xFF1C1C1E.toInt()
    val SUB = 0xFF8E8E93.toInt()
    val BLUE = 0xFF0A84FF.toInt()
    val GREEN = 0xFF34C759.toInt()
    val RED = 0xFFFF3B30.toInt()
    val ORANGE = 0xFFFF9F0A.toInt()
}

const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

fun shape(color: Int, radius: Float): GradientDrawable =
    GradientDrawable().apply { setColor(color); cornerRadius = radius }

fun Context.tv(s: String, size: Float = 16f, color: Int = C.TEXT, bold: Boolean = false): TextView =
    TextView(this).apply {
        text = s
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(Typeface.DEFAULT_BOLD)
    }

fun Context.button(s: String, bg: Int = C.BLUE, fg: Int = Color.WHITE, onClick: () -> Unit): TextView =
    tv(s, 16f, fg, true).apply {
        gravity = Gravity.CENTER
        setPadding(dp(16), dp(12), dp(16), dp(12))
        background = shape(bg, dp(14).toFloat())
        setOnClickListener { onClick() }
    }

fun Context.card(): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    setPadding(dp(16), dp(14), dp(16), dp(14))
    background = shape(C.CARD, dp(18).toFloat())
}

fun lp(w: Int, h: Int, weight: Float = 0f, l: Int = 0, t: Int = 0, r: Int = 0, b: Int = 0) =
    LinearLayout.LayoutParams(w, h, weight).apply { setMargins(l, t, r, b) }

fun Activity.bars(lightBg: Boolean) {
    window.statusBarColor = Color.TRANSPARENT
    @Suppress("DEPRECATION")
    window.decorView.systemUiVisibility = if (lightBg) View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR else 0
}

fun Context.openApp(cls: Class<*>) {
    startActivity(Intent(this, cls).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

fun Context.toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
