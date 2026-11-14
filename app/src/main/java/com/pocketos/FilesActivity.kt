package com.pocketos

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.format.Formatter
import android.webkit.MimeTypeMap
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import androidx.core.content.FileProvider
import java.io.File

class FilesActivity : Activity() {
    private val top: File = Environment.getExternalStorageDirectory()
    private var dir: File = top

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        bars(true)
    }

    override fun onResume() {
        super.onResume()
        show()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val p = dir.parentFile
        if (p != null && dir.path != top.path) { dir = p; show() } else super.onBackPressed()
    }

    private fun hasAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= 30) Environment.isExternalStorageManager()
        else checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    private fun requestAccess() {
        if (Build.VERSION.SDK_INT >= 30)
            startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName")))
        else requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 3)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        show()
    }

    private fun show() {
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(16), dp(20), dp(16), 0)
        root.addView(tv("Files", 32f, C.TEXT, true))
        root.addView(tv(dir.path.removePrefix(top.path).ifEmpty { "/" }, 13f, C.SUB), lp(WRAP, WRAP, 0f, 0, 0, 0, dp(8)))
        if (!hasAccess()) {
            root.addView(tv("Allow file access to browse your storage.", 14f, C.SUB), lp(WRAP, WRAP, 0f, 0, 0, 0, dp(12)))
            root.addView(button("Grant access") { requestAccess() })
            setContentView(root)
            return
        }
        val files = (dir.listFiles() ?: emptyArray())
            .sortedWith(compareBy<File>({ !it.isDirectory }, { it.name.lowercase() }))
        val labels = files.map {
            if (it.isDirectory) "▸ ${it.name}" else "${it.name}  ·  ${Formatter.formatShortFileSize(this, it.length())}"
        }
        val lv = ListView(this)
        lv.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, labels)
        lv.setOnItemClickListener { _, _, pos, _ ->
            val f = files[pos]
            if (f.isDirectory) { dir = f; show() } else openFile(f)
        }
        lv.setOnItemLongClickListener { _, _, pos, _ ->
            val f = files[pos]
            AlertDialog.Builder(this).setMessage("Delete ${f.name}?")
                .setPositiveButton("Delete") { _, _ -> f.deleteRecursively(); show() }
                .setNegativeButton("Cancel", null).show()
            true
        }
        root.addView(lv, lp(MATCH, 0, 1f))
        setContentView(root)
    }

    private fun openFile(f: File) {
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.files", f)
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(f.extension.lowercase()) ?: "*/*"
            startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        } catch (e: Exception) { toast("No app can open this file") }
    }
}
