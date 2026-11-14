package com.pocketos

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var prefs: Prefs

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        prefs = Prefs(this)
        bars(true)
    }

    override fun onResume() {
        super.onResume()
        build()
    }

    private fun header(s: String): TextView =
        tv(s.uppercase(), 13f, C.SUB, true).apply { setPadding(dp(4), dp(18), 0, dp(6)) }

    private fun safeStart(i: Intent) {
        try { startActivity(i) } catch (e: Exception) { toast("Not available on this device") }
    }

    private fun build() {
        val sv = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(20), dp(16), dp(40))
        }
        sv.addView(root)
        setContentView(sv)

        root.addView(tv("PocketOS", 32f, C.TEXT, true))
        root.addView(tv("Light apps · Icon packs · Web apps", 14f, C.SUB), lp(WRAP, WRAP, 0f, 0, dp(2), 0, dp(8)))

        // ---- Apps
        root.addView(header("Apps"))
        val apps = card()
        IconFactory.apps.chunked(4).forEach { row ->
            val r = LinearLayout(this)
            row.forEach { a -> r.addView(cell(a), lp(0, WRAP, 1f)) }
            repeat(4 - row.size) { r.addView(View(this), lp(0, 1, 1f)) }
            apps.addView(r, lp(MATCH, WRAP, 0f, 0, dp(6), 0, dp(6)))
        }
        root.addView(apps)

        // ---- Icon packs
        root.addView(header("Icon packs"))
        IconFactory.packs.forEach { p ->
            val h = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
            listOf("calc", "clock", "dialer", "messages").forEach { id ->
                val iv = ImageView(this)
                iv.setImageBitmap(IconFactory.appIcon(id, p.id, dp(40)))
                h.addView(iv, lp(dp(40), dp(40), 0f, 0, 0, dp(6), 0))
            }
            h.addView(tv(p.name, 16f, C.TEXT, true), lp(0, WRAP, 1f, dp(8), 0, 0, 0))
            val sel = p.id == prefs.pack
            h.addView(button(if (sel) "Selected" else "Use", if (sel) C.SUB else C.BLUE) {
                prefs.pack = p.id
                build()
            }, lp(WRAP, WRAP))
            root.addView(card().apply { addView(h) }, lp(MATCH, WRAP, 0f, 0, 0, 0, dp(8)))
        }
        root.addView(button("Pin all apps to home screen") {
            IconFactory.apps.forEach { Shortcuts.pinApp(this, it, prefs.pack) }
        })
        root.addView(
            tv("Tip: long-press one icon above to pin only that app with the selected pack. " +
                "PocketOS Launcher applies packs to every icon automatically.", 12f, C.SUB),
            lp(MATCH, WRAP, 0f, dp(4), dp(8), dp(4), 0)
        )

        // ---- Web apps
        root.addView(header("Web apps"))
        val w = card()
        val urlEt = EditText(this).apply {
            hint = "https://example.com"
            setSingleLine()
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        }
        val nameEt = EditText(this).apply { hint = "Name (optional)"; setSingleLine() }
        w.addView(urlEt)
        w.addView(nameEt)
        w.addView(button("Create app") { createWeb(urlEt.text.toString(), nameEt.text.toString()) },
            lp(MATCH, WRAP, 0f, 0, dp(10), 0, dp(6)))
        prefs.webApps().forEach { (t, u) -> w.addView(webRow(t, u), lp(MATCH, WRAP, 0f, 0, dp(6), 0, 0)) }
        root.addView(w)

        // ---- System
        root.addView(header("System"))
        val sys = card()
        sys.addView(button("Set PocketOS Launcher as home") { safeStart(Intent(Settings.ACTION_HOME_SETTINGS)) })
        sys.addView(button("Default apps (phone / SMS)") {
            safeStart(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
        }, lp(MATCH, WRAP, 0f, 0, dp(8), 0, 0))
        sys.addView(button("Open Control Center") { openApp(ControlCenterActivity::class.java) },
            lp(MATCH, WRAP, 0f, 0, dp(8), 0, 0))
        root.addView(sys)
    }

    private fun cell(a: IconFactory.AppDef): View {
        val l = LinearLayout(this)
        l.orientation = LinearLayout.VERTICAL
        l.gravity = Gravity.CENTER_HORIZONTAL
        val iv = ImageView(this)
        iv.setImageBitmap(IconFactory.appIcon(a.id, prefs.pack, dp(56)))
        l.addView(iv, lp(dp(56), dp(56)))
        val t = tv(a.label, 11f, C.SUB)
        t.gravity = Gravity.CENTER
        t.maxLines = 1
        l.addView(t, lp(WRAP, WRAP, 0f, 0, dp(4), 0, 0))
        l.setOnClickListener { openApp(a.cls) }
        l.setOnLongClickListener { Shortcuts.pinApp(this, a, prefs.pack); true }
        return l
    }

    private fun webRow(t: String, u: String): View {
        val r = LinearLayout(this)
        r.gravity = Gravity.CENTER_VERTICAL
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.addView(tv(t, 16f, C.TEXT, true))
        val sub = tv(u, 12f, C.SUB)
        sub.maxLines = 1
        col.addView(sub)
        r.addView(col, lp(0, WRAP, 1f))
        r.addView(button("✕", C.CARD, C.RED) { prefs.removeWeb(u); build() })
        r.setOnClickListener {
            startActivity(Shortcuts.webIntent(this, t, u).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        r.setOnLongClickListener { pinWeb(t, u); true }
        return r
    }

    private fun createWeb(rawUrl: String, rawName: String) {
        var u = rawUrl.trim()
        if (u.isEmpty()) return
        if (!u.startsWith("http://") && !u.startsWith("https://")) u = "https://$u"
        val host = Uri.parse(u).host
        if (host.isNullOrEmpty()) { toast("Invalid URL"); return }
        val name = rawName.trim().ifEmpty {
            host.removePrefix("www.").substringBeforeLast('.').replaceFirstChar { it.uppercase() }
        }
        prefs.addWeb(name, u)
        pinWeb(name, u)
    }

    private fun pinWeb(name: String, u: String) {
        val host = Uri.parse(u).host ?: return
        Thread {
            val fav = Shortcuts.favicon(host)
            runOnUiThread {
                Shortcuts.pinWeb(this, name, u, IconFactory.webIcon(name, fav, prefs.pack, 192))
                build()
            }
        }.start()
    }
}
