package com.pocketos

import android.app.Activity
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView

class LauncherActivity : Activity() {
    private lateinit var prefs: Prefs
    private val cache = HashMap<String, Bitmap>()
    private var lastPack = ""
    private var lastCount = -1
    private var scroll: ScrollView? = null
    private var downX = 0f
    private var downY = 0f

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        prefs = Prefs(this)
    }

    private fun items(): List<ResolveInfo> {
        val pm = packageManager
        return pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .sortedBy { it.loadLabel(pm).toString().lowercase() }
    }

    override fun onResume() {
        super.onResume()
        val list = items()
        if (scroll == null || prefs.pack != lastPack || list.size != lastCount) build(list)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        scroll?.scrollTo(0, 0)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Home screen: ignore back
    }

    // Swipe down on the upper-right part of the home screen opens Control Center
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> { downX = ev.x; downY = ev.y }
            MotionEvent.ACTION_UP -> {
                val w = resources.displayMetrics.widthPixels
                val h = resources.displayMetrics.heightPixels
                val dy = ev.y - downY
                val dx = ev.x - downX
                if (downX > w / 2 && downY < h * 0.4f && dy > dp(90) &&
                    Math.abs(dx) < dy / 2 && (scroll?.scrollY ?: 0) == 0
                ) openApp(ControlCenterActivity::class.java)
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun iconFor(ri: ResolveInfo, packId: String, px: Int): Bitmap {
        val key = ri.activityInfo.packageName + "/" + ri.activityInfo.name + "/" + packId
        return cache.getOrPut(key) {
            val own = IconFactory.apps.firstOrNull { it.cls.name == ri.activityInfo.name }
            if (ri.activityInfo.packageName == packageName && own != null) IconFactory.appIcon(own.id, packId, px)
            else IconFactory.masked(ri.loadIcon(packageManager), packId, px)
        }
    }

    private fun build(list: List<ResolveInfo>) {
        val packId = prefs.pack
        if (packId != lastPack) { cache.clear(); lastPack = packId }
        lastCount = list.size

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(0, dp(12), 0, dp(10))

        val sv = ScrollView(this)
        sv.isVerticalScrollBarEnabled = false
        scroll = sv
        val grid = LinearLayout(this)
        grid.orientation = LinearLayout.VERTICAL
        grid.setPadding(dp(8), 0, dp(8), dp(8))
        list.chunked(4).forEach { row ->
            val r = LinearLayout(this)
            row.forEach { r.addView(cell(it, packId), lp(0, WRAP, 1f)) }
            repeat(4 - row.size) { r.addView(View(this), lp(0, 1, 1f)) }
            grid.addView(r, lp(MATCH, WRAP, 0f, 0, 0, 0, dp(18)))
        }
        sv.addView(grid)
        root.addView(sv, lp(MATCH, 0, 1f))

        val dock = LinearLayout(this)
        dock.gravity = Gravity.CENTER
        dock.setPadding(dp(8), dp(12), dp(8), dp(12))
        dock.background = shape(0x55FFFFFF, dp(30).toFloat())
        listOf("dialer", "messages", "contacts", "hub").forEach { id ->
            val a = IconFactory.apps.first { it.id == id }
            val iv = ImageView(this)
            iv.setImageBitmap(IconFactory.appIcon(id, packId, dp(60)))
            iv.setOnClickListener { openApp(a.cls) }
            dock.addView(iv, lp(0, dp(60), 1f))
        }
        root.addView(dock, lp(MATCH, WRAP, 0f, dp(12), 0, dp(12), 0))
        setContentView(root)
    }

    private fun cell(ri: ResolveInfo, packId: String): View {
        val l = LinearLayout(this)
        l.orientation = LinearLayout.VERTICAL
        l.gravity = Gravity.CENTER_HORIZONTAL
        val iv = ImageView(this)
        iv.setImageBitmap(iconFor(ri, packId, dp(60)))
        l.addView(iv, lp(dp(60), dp(60)))
        val t = tv(ri.loadLabel(packageManager).toString(), 11f, Color.WHITE)
        t.gravity = Gravity.CENTER
        t.maxLines = 1
        t.ellipsize = TextUtils.TruncateAt.END
        t.setShadowLayer(4f, 0f, 1f, 0x99000000.toInt())
        l.addView(t, lp(MATCH, WRAP, 0f, dp(2), dp(4), dp(2), 0))
        l.setOnClickListener {
            try {
                startActivity(
                    Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                        .setClassName(ri.activityInfo.packageName, ri.activityInfo.name)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (e: Exception) { toast("Cannot open") }
        }
        l.setOnLongClickListener {
            try {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + ri.activityInfo.packageName)))
            } catch (e: Exception) { }
            true
        }
        return l
    }
}
