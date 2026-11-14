package com.pocketos

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import java.io.File

class NotesActivity : Activity() {
    private var cur: File? = null
    private var editor: EditText? = null
    private val dir: File get() = File(filesDir, "notes").also { it.mkdirs() }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        bars(true)
    }

    override fun onResume() {
        super.onResume()
        if (cur == null) showList()
    }

    override fun onPause() {
        saveCur()
        super.onPause()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (cur != null) { saveCur(); cur = null; showList() } else super.onBackPressed()
    }

    private fun saveCur() {
        val f = cur ?: return
        val t = editor?.text?.toString() ?: return
        if (t.isBlank()) f.delete() else f.writeText(t)
    }

    private fun showList() {
        editor = null
        val sv = ScrollView(this)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(16), dp(20), dp(16), dp(32))
        sv.addView(root)

        val top = LinearLayout(this)
        top.gravity = Gravity.CENTER_VERTICAL
        top.addView(tv("Notes", 32f, C.TEXT, true), lp(0, WRAP, 1f))
        top.addView(button("New") { edit(null) })
        root.addView(top, lp(MATCH, WRAP, 0f, 0, 0, 0, dp(12)))

        val files = (dir.listFiles() ?: emptyArray()).sortedByDescending { it.lastModified() }
        if (files.isEmpty()) root.addView(tv("No notes yet", 14f, C.SUB))
        files.forEach { f ->
            val text = f.readText()
            val c = card()
            c.addView(tv(text.lineSequence().firstOrNull()?.take(60) ?: "", 16f, C.TEXT, true))
            val prev = tv(text.lineSequence().drop(1).joinToString(" ").take(120), 13f, C.SUB)
            prev.maxLines = 2
            prev.ellipsize = TextUtils.TruncateAt.END
            c.addView(prev)
            c.setOnClickListener { edit(f) }
            c.setOnLongClickListener {
                AlertDialog.Builder(this).setMessage("Delete this note?")
                    .setPositiveButton("Delete") { _, _ -> f.delete(); showList() }
                    .setNegativeButton("Cancel", null).show()
                true
            }
            root.addView(c, lp(MATCH, WRAP, 0f, 0, 0, 0, dp(8)))
        }
        setContentView(sv)
    }

    private fun edit(f: File?) {
        cur = f ?: File(dir, "${System.currentTimeMillis()}.txt")
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(16), dp(16), dp(16), dp(16))
        val top = LinearLayout(this)
        top.gravity = Gravity.CENTER_VERTICAL
        top.addView(View(this), lp(0, 1, 1f))
        top.addView(button("Done") { saveCur(); cur = null; showList() })
        root.addView(top)
        val e = EditText(this)
        e.setText(f?.readText() ?: "")
        e.gravity = Gravity.TOP
        e.setBackgroundColor(0)
        e.textSize = 18f
        editor = e
        root.addView(e, lp(MATCH, 0, 1f, 0, dp(8), 0, 0))
        setContentView(root)
        e.requestFocus()
    }
}
