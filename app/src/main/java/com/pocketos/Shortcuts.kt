package com.pocketos

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.net.Uri
import java.net.HttpURLConnection
import java.net.URL

object Shortcuts {
    private fun sm(ctx: Context): ShortcutManager? {
        val m = ctx.getSystemService(ShortcutManager::class.java)
        if (m == null || !m.isRequestPinShortcutSupported) {
            ctx.toast("Your launcher does not support pinned shortcuts")
            return null
        }
        return m
    }

    fun pinApp(ctx: Context, app: IconFactory.AppDef, packId: String) {
        val m = sm(ctx) ?: return
        val info = ShortcutInfo.Builder(ctx, "app_${app.id}_$packId")
            .setShortLabel(app.label)
            .setIcon(Icon.createWithBitmap(IconFactory.appIcon(app.id, packId, 192)))
            .setIntent(Intent(ctx, app.cls).setAction(Intent.ACTION_MAIN))
            .build()
        m.requestPinShortcut(info, null)
    }

    fun webIntent(ctx: Context, title: String, url: String): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(url), ctx, WebAppActivity::class.java).putExtra("title", title)

    fun pinWeb(ctx: Context, title: String, url: String, icon: Bitmap) {
        val m = sm(ctx) ?: return
        val info = ShortcutInfo.Builder(ctx, "web_${url.hashCode()}_${System.currentTimeMillis() % 100000}")
            .setShortLabel(title)
            .setIcon(Icon.createWithBitmap(icon))
            .setIntent(webIntent(ctx, title, url))
            .build()
        m.requestPinShortcut(info, null)
    }

    fun favicon(host: String): Bitmap? = try {
        val con = URL("https://www.google.com/s2/favicons?domain=$host&sz=128").openConnection() as HttpURLConnection
        con.connectTimeout = 5000
        con.readTimeout = 5000
        con.inputStream.use { BitmapFactory.decodeStream(it) }
    } catch (e: Exception) {
        null
    }
}
