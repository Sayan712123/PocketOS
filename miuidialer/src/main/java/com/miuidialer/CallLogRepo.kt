package com.miuidialer

import android.content.Context
import android.provider.CallLog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object CallLogRepo {

    data class Entry(
        val number: String,
        val name: String?,
        val type: Int,
        val date: Long,
        val duration: Long
    )

    fun recent(ctx: Context, limit: Int = 300): List<Entry> {
        val out = ArrayList<Entry>()
        try {
            ctx.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DATE,
                    CallLog.Calls.DURATION
                ),
                null, null,
                CallLog.Calls.DATE + " DESC"
            )?.use { c ->
                val ni = c.getColumnIndex(CallLog.Calls.NUMBER)
                val ci = c.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val ti = c.getColumnIndex(CallLog.Calls.TYPE)
                val di = c.getColumnIndex(CallLog.Calls.DATE)
                val ui = c.getColumnIndex(CallLog.Calls.DURATION)
                while (c.moveToNext() && out.size < limit) {
                    val num = c.getString(ni) ?: continue
                    out.add(
                        Entry(
                            num,
                            c.getString(ci),
                            c.getInt(ti),
                            c.getLong(di),
                            c.getLong(ui)
                        )
                    )
                }
            }
        } catch (e: Exception) { }
        return out
    }

    fun label(type: Int): String = when (type) {
        CallLog.Calls.INCOMING_TYPE -> "Incoming"
        CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
        CallLog.Calls.MISSED_TYPE -> "Missed"
        CallLog.Calls.REJECTED_TYPE -> "Rejected"
        CallLog.Calls.BLOCKED_TYPE -> "Blocked"
        else -> "Call"
    }

    fun arrow(type: Int): String = when (type) {
        CallLog.Calls.INCOMING_TYPE -> "\u2199"
        CallLog.Calls.OUTGOING_TYPE -> "\u2197"
        CallLog.Calls.MISSED_TYPE -> "\u2715"
        CallLog.Calls.REJECTED_TYPE -> "\u2298"
        else -> "\u2022"
    }

    fun color(type: Int): Int = when (type) {
        CallLog.Calls.MISSED_TYPE, CallLog.Calls.REJECTED_TYPE, CallLog.Calls.BLOCKED_TYPE -> M.RED
        CallLog.Calls.OUTGOING_TYPE -> M.BLUE
        else -> M.GREEN
    }

    fun whenText(ts: Long): String {
        val now = Calendar.getInstance()
        val then = Calendar.getInstance().apply { timeInMillis = ts }
        val sameDay = now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
        val yesterday = now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) - then.get(Calendar.DAY_OF_YEAR) == 1
        return when {
            sameDay -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))
            yesterday -> "Yesterday"
            else -> SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(ts))
        }
    }

    fun durationText(sec: Long): String {
        if (sec <= 0) return ""
        val m = sec / 60
        val s = sec % 60
        return if (m > 0) "${m}m ${s}s" else "${s}s"
    }
}
