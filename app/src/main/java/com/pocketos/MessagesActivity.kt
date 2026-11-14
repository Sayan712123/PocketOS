package com.pocketos

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.telephony.SmsManager
import android.text.TextUtils
import android.text.format.DateUtils
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MessagesActivity : Activity() {
    private class Msg(val thread: Long, val addr: String, val body: String, val date: Long, val mine: Boolean)

    private var inThread = false
    private var pendingAddr = ""

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        bars(true)
        pendingAddr = intent?.data?.schemeSpecificPart ?: ""
        if (checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) start()
        else requestPermissions(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS), 7)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) start()
        else toast("SMS permission needed")
    }

    private fun start() { if (pendingAddr.isNotEmpty()) showThread(pendingAddr, -1) else showList() }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { if (inThread) showList() else super.onBackPressed() }

    private fun load(where: String?, args: Array<String>?, order: String): List<Msg> {
        val out = ArrayList<Msg>()
        try {
            contentResolver.query(
                Telephony.Sms.CONTENT_URI, arrayOf("thread_id", "address", "body", "date", "type"), where, args, order
            )?.use { c ->
                while (c.moveToNext())
                    out.add(Msg(c.getLong(0), c.getString(1) ?: "", c.getString(2) ?: "", c.getLong(3), c.getInt(4) == 2))
            }
        } catch (e: Exception) { toast("Cannot read SMS") }
        return out
    }

    private fun showList() {
        inThread = false
        val seen = LinkedHashMap<Long, Msg>()
        load(null, null, "date DESC LIMIT 400").forEach { if (!seen.containsKey(it.thread)) seen[it.thread] = it }
        val sv = ScrollView(this)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(16), dp(20), dp(16), dp(32))
        sv.addView(root)
        val top = LinearLayout(this)
        top.gravity = Gravity.CENTER_VERTICAL
        top.addView(tv("Messages", 32f, C.TEXT, true), lp(0, WRAP, 1f))
        top.addView(button("New") { showThread("", -1) })
        root.addView(top, lp(MATCH, WRAP, 0f, 0, 0, 0, dp(12)))
        seen.values.forEach { m ->
            val c = card()
            c.addView(tv(m.addr, 16f, C.TEXT, true))
            val body = tv(m.body, 14f, C.SUB)
            body.maxLines = 1
            body.ellipsize = TextUtils.TruncateAt.END
            c.addView(body)
            c.addView(tv(DateUtils.getRelativeTimeSpanString(m.date).toString(), 11f, C.SUB))
            c.setOnClickListener { showThread(m.addr, m.thread) }
            root.addView(c, lp(MATCH, WRAP, 0f, 0, 0, 0, dp(8)))
        }
        setContentView(sv)
    }

    private fun bubble(box: LinearLayout, text: String, mine: Boolean) {
        val t: TextView = tv(text, 16f, if (mine) android.graphics.Color.WHITE else C.TEXT)
        t.setPadding(dp(14), dp(8), dp(14), dp(8))
        t.background = shape(if (mine) C.BLUE else 0xFFE5E5EA.toInt(), dp(18).toFloat())
        t.maxWidth = (resources.displayMetrics.widthPixels * 0.75f).toInt()
        val p = lp(WRAP, WRAP, 0f, 0, dp(3), 0, dp(3))
        p.gravity = if (mine) Gravity.END else Gravity.START
        box.addView(t, p)
    }

    private fun showThread(addr0: String, thread: Long) {
        inThread = true
        val msgs = if (thread >= 0) load("thread_id=?", arrayOf(thread.toString()), "date ASC") else emptyList()
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(12), dp(16), dp(12), dp(12))
        val top = LinearLayout(this)
        top.gravity = Gravity.CENTER_VERTICAL
        top.addView(button("‹", C.BG, C.BLUE) { showList() })
        var addrEt: EditText? = null
        if (addr0.isEmpty()) {
            addrEt = EditText(this)
            addrEt.hint = "To (number)"
            addrEt.setSingleLine()
            addrEt.inputType = android.text.InputType.TYPE_CLASS_PHONE
            top.addView(addrEt, lp(0, WRAP, 1f))
        } else top.addView(tv(addr0, 20f, C.TEXT, true), lp(0, WRAP, 1f))
        root.addView(top)

        val sv = ScrollView(this)
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(4), dp(8), dp(4), dp(8))
        sv.addView(box)
        msgs.forEach { bubble(box, it.body, it.mine) }
        root.addView(sv, lp(MATCH, 0, 1f))
        sv.post { sv.fullScroll(View.FOCUS_DOWN) }

        val bar = LinearLayout(this)
        bar.gravity = Gravity.CENTER_VERTICAL
        val input = EditText(this)
        input.hint = "Message"
        input.maxLines = 4
        bar.addView(input, lp(0, WRAP, 1f))
        bar.addView(button("Send") {
            val to = (addrEt?.text?.toString() ?: addr0).trim()
            val body = input.text.toString()
            if (to.isNotEmpty() && body.isNotBlank()) {
                try {
                    val sm = sms()
                    sm.sendMultipartTextMessage(to, null, sm.divideMessage(body), null, null)
                    bubble(box, body, true)
                    input.setText("")
                    sv.post { sv.fullScroll(View.FOCUS_DOWN) }
                } catch (e: Exception) { toast("Send failed") }
            }
        })
        root.addView(bar)
        setContentView(root)
    }

    @Suppress("DEPRECATION")
    private fun sms(): SmsManager =
        if (Build.VERSION.SDK_INT >= 31) getSystemService(SmsManager::class.java) else SmsManager.getDefault()
}
