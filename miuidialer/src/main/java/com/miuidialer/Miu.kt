package com.miuidialer

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

/** MIUI (Xiaomi / POCO) colour + layout tokens. */
object M {
    val BLUE = 0xFF3482FF.toInt()
    val BG = 0xFFFFFFFF.toInt()
    val TEXT = 0xFF000000.toInt()
    val SUB = 0xFF9A9A9A.toInt()
    val DIV = 0xFFEFEFEF.toInt()
    val KEY = 0xFFF6F6F6.toInt()
    val GREEN = 0xFF12B76A.toInt()
    val RED = 0xFFF04438.toInt()
    val DARK = 0xFF0E1418.toInt()
    val DARK2 = 0xFF1B2733.toInt()
    val WHITE = 0xFFFFFFFF.toInt()
    val GLASS = 0x22FFFFFF.toInt()
}

const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

fun shape(color: Int, radius: Float): GradientDrawable =
    GradientDrawable().apply { setColor(color); cornerRadius = radius }

fun circle(color: Int): GradientDrawable =
    GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(color) }

fun Context.tv(s: String, size: Float = 16f, color: Int = M.TEXT, bold: Boolean = false): TextView =
    TextView(this).apply {
        text = s
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(Typeface.DEFAULT_BOLD)
    }

fun lp(w: Int, h: Int, weight: Float = 0f, l: Int = 0, t: Int = 0, r: Int = 0, b: Int = 0) =
    LinearLayout.LayoutParams(w, h, weight).apply { setMargins(l, t, r, b) }

fun Activity.lightBars() {
    window.statusBarColor = M.BG
    window.navigationBarColor = M.BG
    @Suppress("DEPRECATION")
    window.decorView.systemUiVisibility =
        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
}

fun Activity.darkBars() {
    window.statusBarColor = Color.TRANSPARENT
    window.navigationBarColor = Color.TRANSPARENT
    @Suppress("DEPRECATION")
    window.decorView.systemUiVisibility = 0
}

fun Context.toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

fun Context.open(cls: Class<*>) {
    startActivity(Intent(this, cls).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/** Small circular icon button with a caption underneath, MIUI call-screen style. */
fun Context.callAction(icon: Int, label: String, bg: Int, fg: Int = M.WHITE, onClick: () -> Unit): LinearLayout {
    val col = LinearLayout(this)
    col.orientation = LinearLayout.VERTICAL
    col.gravity = Gravity.CENTER_HORIZONTAL
    val iv = android.widget.ImageView(this)
    iv.setImageResource(icon)
    iv.setColorFilter(fg)
    iv.setPadding(dp(20), dp(20), dp(20), dp(20))
    iv.background = circle(bg)
    col.addView(iv, lp(dp(64), dp(64)))
    val t = tv(label, 12f, 0xCCFFFFFF.toInt())
    t.gravity = Gravity.CENTER
    col.addView(t, lp(WRAP, WRAP, 0f, 0, dp(8), 0, 0))
    col.setOnClickListener { onClick() }
    return col
}
