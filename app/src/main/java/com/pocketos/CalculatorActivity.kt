package com.pocketos

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import java.math.BigDecimal
import java.math.RoundingMode

private class Parser(val s: String) {
    var i = 0
    fun expr(): Double {
        var v = term()
        while (i < s.length && (s[i] == '+' || s[i] == '-')) {
            val o = s[i++]
            val t = term()
            v = if (o == '+') v + t else v - t
        }
        return v
    }
    private fun term(): Double {
        var v = unary()
        while (i < s.length && (s[i] == '×' || s[i] == '÷')) {
            val o = s[i++]
            val t = unary()
            v = if (o == '×') v * t else v / t
        }
        return v
    }
    private fun unary(): Double {
        if (i < s.length && s[i] == '-') { i++; return -unary() }
        return num()
    }
    private fun num(): Double {
        val st = i
        while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
        var v = s.substring(st, i).toDouble()
        while (i < s.length && s[i] == '%') { i++; v /= 100 }
        return v
    }
}

class CalculatorActivity : Activity() {
    private var expr = ""
    private lateinit var disp: TextView
    private val ops = "+-×÷"

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        bars(false)
        window.decorView.setBackgroundColor(Color.BLACK)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.BLACK)
        root.setPadding(dp(10), dp(10), dp(10), dp(14))
        disp = tv("0", 64f, Color.WHITE)
        disp.gravity = Gravity.END or Gravity.BOTTOM
        disp.maxLines = 1
        disp.setPadding(dp(12), 0, dp(12), dp(8))
        disp.setAutoSizeTextTypeUniformWithConfiguration(28, 64, 2, TypedValue.COMPLEX_UNIT_SP)
        root.addView(disp, lp(MATCH, 0, 1.1f))
        val rows = listOf(
            listOf("AC", "⌫", "%", "÷"), listOf("7", "8", "9", "×"), listOf("4", "5", "6", "-"),
            listOf("1", "2", "3", "+"), listOf("0", ".", "=")
        )
        rows.forEach { r ->
            val row = LinearLayout(this)
            r.forEach { k -> row.addView(key(k), lp(0, MATCH, if (k == "0") 2f else 1f, dp(5), dp(5), dp(5), dp(5))) }
            root.addView(row, lp(MATCH, 0, 1f))
        }
        setContentView(root)
    }

    private fun key(k: String): TextView {
        val gray = 0xFFA5A5A5.toInt()
        val bg = when (k) {
            "÷", "×", "-", "+", "=" -> C.ORANGE
            "AC", "⌫", "%" -> gray
            else -> 0xFF333333.toInt()
        }
        val t = tv(k, 28f, if (bg == gray) Color.BLACK else Color.WHITE)
        t.gravity = Gravity.CENTER
        t.background = shape(bg, dp(40).toFloat())
        t.setOnClickListener { press(k) }
        return t
    }

    private fun press(k: String) {
        if (expr == "Error") expr = ""
        when (k) {
            "AC" -> expr = ""
            "⌫" -> expr = expr.dropLast(1)
            "=" -> if (expr.isNotEmpty()) expr = calc(expr)
            else -> {
                if (k.length == 1 && ops.contains(k)) {
                    if (expr.isEmpty() && k != "-") return
                    val last = expr.lastOrNull()
                    if (last != null && ops.contains(last)) expr = expr.dropLast(1)
                }
                expr += k
            }
        }
        disp.text = if (expr.isEmpty()) "0" else expr
    }

    private fun calc(s: String): String = try {
        val p = Parser(s)
        val v = p.expr()
        if (p.i != s.length || v.isNaN() || v.isInfinite()) "Error" else fmt(v)
    } catch (e: Exception) {
        "Error"
    }

    private fun fmt(v: Double): String =
        if (v == Math.floor(v) && Math.abs(v) < 1e15) v.toLong().toString()
        else BigDecimal(v).setScale(8, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
}
