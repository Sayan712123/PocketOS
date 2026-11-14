package com.pocketos

import android.graphics.*
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable

object IconFactory {
    class Pack(val id: String, val name: String, val circle: Boolean, val mode: Int) // mode: 0 colour, 1 mono dark, 2 light
    class AppDef(val id: String, val label: String, val color: Int, val cls: Class<*>)

    val packs = listOf(
        Pack("ios", "iOS", false, 0),
        Pack("round", "Round", true, 0),
        Pack("mono", "Mono", false, 1),
        Pack("light", "Light", false, 2)
    )

    val apps = listOf(
        AppDef("calc", "Calculator", 0xFF1C1C1E.toInt(), CalculatorActivity::class.java),
        AppDef("clock", "Clock", 0xFF1C1C1E.toInt(), ClockActivity::class.java),
        AppDef("dialer", "Dialer", 0xFF34C759.toInt(), DialerActivity::class.java),
        AppDef("contacts", "Contacts", 0xFF8E8E93.toInt(), ContactsActivity::class.java),
        AppDef("messages", "Messages", 0xFF0A84FF.toInt(), MessagesActivity::class.java),
        AppDef("notes", "Notes", 0xFFFFB300.toInt(), NotesActivity::class.java),
        AppDef("files", "Files", 0xFF30B0C7.toInt(), FilesActivity::class.java),
        AppDef("hub", "PocketOS", 0xFF5E5CE6.toInt(), MainActivity::class.java)
    )

    fun pack(id: String): Pack = packs.firstOrNull { it.id == id } ?: packs[0]

    private fun bgPath(s: Float, circle: Boolean): Path = Path().apply {
        if (circle) addCircle(s / 2, s / 2, s / 2, Path.Direction.CW)
        else addRoundRect(RectF(0f, 0f, s, s), s * 0.235f, s * 0.235f, Path.Direction.CW)
    }

    fun appIcon(id: String, packId: String, size: Int): Bitmap {
        val app = apps.first { it.id == id }
        val p = pack(packId)
        val s = size.toFloat()
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val bg = when (p.mode) { 1 -> 0xFF1C1C1E.toInt(); 2 -> Color.WHITE; else -> app.color }
        val fg = if (p.mode == 2) app.color else Color.WHITE
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = bg
        c.drawPath(bgPath(s, p.circle), paint)
        if (p.mode == 2) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = s * 0.015f
            paint.color = 0x33000000
            c.drawPath(bgPath(s, p.circle), paint)
        }
        c.save()
        c.scale(s / 64f, s / 64f)
        glyph(c, id, fg)
        c.restore()
        return bmp
    }

    private fun glyph(c: Canvas, id: String, fg: Int) {
        val f = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fg }
        val st = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fg; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeWidth = 4f
        }
        val p = Path()
        when (id) {
            "calc" -> {
                c.drawRoundRect(RectF(10f, 10f, 54f, 22f), 3f, 3f, f)
                for (r in 0..2) for (k in 0..2) c.drawCircle(18f + 14f * k, 32f + 10f * r, 4.2f, f)
            }
            "clock" -> {
                c.drawCircle(32f, 32f, 22f, st)
                c.drawLine(32f, 32f, 32f, 18f, st)
                c.drawLine(32f, 32f, 42f, 38f, st)
            }
            "dialer" -> {
                p.moveTo(20f, 18f); p.quadTo(22f, 14f, 26f, 16f); p.lineTo(30f, 21f); p.quadTo(31f, 24f, 29f, 26f)
                p.lineTo(27f, 28f); p.quadTo(31f, 37f, 38f, 41f); p.lineTo(40f, 39f); p.quadTo(42f, 37f, 45f, 38f)
                p.lineTo(50f, 42f); p.quadTo(52f, 46f, 48f, 49f); p.quadTo(44f, 52f, 38f, 49f)
                p.quadTo(22f, 42f, 16f, 26f); p.quadTo(15f, 21f, 20f, 18f); p.close()
                c.drawPath(p, f)
            }
            "contacts" -> {
                c.drawCircle(32f, 24f, 8f, f)
                p.moveTo(15f, 54f); p.quadTo(15f, 36f, 32f, 36f); p.quadTo(49f, 36f, 49f, 54f); p.close()
                c.drawPath(p, f)
            }
            "messages" -> {
                c.drawOval(RectF(11f, 14f, 53f, 44f), f)
                p.moveTo(19f, 40f); p.lineTo(14f, 52f); p.lineTo(28f, 43f); p.close()
                c.drawPath(p, f)
            }
            "notes" -> for (i in 0..2) c.drawRoundRect(RectF(14f, 20f + 11f * i, if (i == 2) 38f else 50f, 24f + 11f * i), 2f, 2f, f)
            "files" -> {
                p.moveTo(12f, 22f); p.lineTo(27f, 22f); p.lineTo(31f, 27f); p.lineTo(52f, 27f)
                p.lineTo(52f, 48f); p.lineTo(12f, 48f); p.close()
                c.drawPath(p, f)
            }
            "hub" -> for (r in 0..1) for (k in 0..1)
                c.drawRoundRect(RectF(12f + 22f * k, 12f + 22f * r, 30f + 22f * k, 30f + 22f * r), 4f, 4f, f)
        }
    }

    fun webIcon(title: String, fav: Bitmap?, packId: String, size: Int): Bitmap {
        val p = pack(packId)
        val s = size.toFloat()
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val palette = intArrayOf(0xFF0A84FF.toInt(), 0xFF34C759.toInt(), 0xFFFF9F0A.toInt(), 0xFFFF375F.toInt(), 0xFF5E5CE6.toInt(), 0xFF30B0C7.toInt())
        val col = palette[(title.hashCode() and 0x7fffffff) % palette.size]
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = when {
            p.mode == 1 -> 0xFF1C1C1E.toInt()
            p.mode == 2 || fav != null -> Color.WHITE
            else -> col
        }
        c.drawPath(bgPath(s, p.circle), paint)
        if (fav != null) {
            val d = s * 0.58f
            c.drawBitmap(fav, null, RectF((s - d) / 2, (s - d) / 2, (s + d) / 2, (s + d) / 2), Paint(Paint.FILTER_BITMAP_FLAG))
        } else {
            val t = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (p.mode == 2) col else Color.WHITE
                textSize = s * 0.5f; textAlign = Paint.Align.CENTER; isFakeBoldText = true
            }
            c.drawText(title.take(1).uppercase(), s / 2, s / 2 - (t.ascent() + t.descent()) / 2, t)
        }
        return bmp
    }

    fun masked(d: Drawable, packId: String, size: Int): Bitmap {
        val p = pack(packId)
        val tmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val cv = Canvas(tmp)
        if (d is AdaptiveIconDrawable) {
            val o = (size * 0.25f).toInt()
            d.background?.let { it.setBounds(-o, -o, size + o, size + o); it.draw(cv) }
            d.foreground?.let { it.setBounds(-o, -o, size + o, size + o); it.draw(cv) }
        } else {
            cv.drawColor(Color.WHITE)
            val i = (size * 0.12f).toInt()
            d.setBounds(i, i, size - i, size - i)
            d.draw(cv)
        }
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = BitmapShader(tmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        Canvas(out).drawPath(bgPath(size.toFloat(), p.circle), paint)
        return out
    }
}
