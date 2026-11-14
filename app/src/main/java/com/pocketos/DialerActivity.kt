package com.pocketos

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class DialerActivity : Activity() {
    private val num = StringBuilder()
    private lateinit var disp: TextView

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        bars(true)
        intent?.data?.schemeSpecificPart?.let { num.append(it) }
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(20), dp(24), dp(20), dp(24))
        disp = tv(num.toString(), 36f, C.TEXT)
        disp.gravity = Gravity.CENTER
        root.addView(disp, lp(MATCH, 0, 1f))

        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#")
        for (r in 0..3) {
            val row = LinearLayout(this)
            for (k in 0..2) {
                val key = keys[r * 3 + k]
                val t = tv(key, 28f, C.TEXT, true)
                t.gravity = Gravity.CENTER
                t.background = shape(C.CARD, dp(36).toFloat())
                t.setOnClickListener { num.append(key); refresh() }
                row.addView(t, lp(0, dp(72), 1f, dp(6), dp(6), dp(6), dp(6)))
            }
            root.addView(row)
        }
        val bottom = LinearLayout(this)
        bottom.gravity = Gravity.CENTER_VERTICAL
        bottom.addView(android.view.View(this), lp(0, 1, 1f))
        bottom.addView(button("Call", C.GREEN) { call() }, lp(0, dp(64), 1.4f, dp(6), dp(6), dp(6), 0))
        val del = button("⌫", C.BG, C.SUB) { if (num.isNotEmpty()) num.deleteCharAt(num.length - 1); refresh() }
        del.setOnLongClickListener { num.setLength(0); refresh(); true }
        bottom.addView(del, lp(0, dp(64), 1f, dp(6), dp(6), dp(6), 0))
        root.addView(bottom)
        setContentView(root)
    }

    private fun refresh() { disp.text = num.toString() }

    private fun call() {
        if (num.isEmpty()) return
        val uri = Uri.parse("tel:" + Uri.encode(num.toString()))
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED)
            startActivity(Intent(Intent.ACTION_CALL, uri))
        else requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), 1)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (num.isEmpty()) return
        val uri = Uri.parse("tel:" + Uri.encode(num.toString()))
        val ok = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        startActivity(Intent(if (ok) Intent.ACTION_CALL else Intent.ACTION_DIAL, uri))
    }
}
