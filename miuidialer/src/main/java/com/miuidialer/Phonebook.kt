package com.miuidialer

import android.content.Context
import android.provider.ContactsContract

object Phonebook {

    data class Contact(val name: String, val number: String)

    private val cache = HashMap<String, String>()

    fun contacts(ctx: Context): List<Contact> {
        val out = ArrayList<Contact>()
        val seen = HashSet<String>()
        try {
            ctx.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )?.use { c ->
                val ni = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val mi = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (c.moveToNext()) {
                    val n = c.getString(ni) ?: continue
                    val num = c.getString(mi) ?: continue
                    val key = n + "|" + digits(num)
                    if (!seen.add(key)) continue
                    out.add(Contact(n, num))
                    cache[digits(num)] = n
                }
            }
        } catch (e: Exception) { }
        return out
    }

    fun digits(s: String): String = s.filter { it.isDigit() }.takeLast(10)

    fun nameFor(ctx: Context, number: String): String? {
        val d = digits(number)
        if (d.isEmpty()) return null
        cache[d]?.let { return it }
        try {
            val uri = android.net.Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                android.net.Uri.encode(number)
            )
            ctx.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null, null, null
            )?.use { c ->
                if (c.moveToFirst()) {
                    val n = c.getString(0)
                    if (!n.isNullOrBlank()) { cache[d] = n; return n }
                }
            }
        } catch (e: Exception) { }
        return null
    }

    fun initial(name: String?): String {
        val s = name?.trim().orEmpty()
        return if (s.isEmpty()) "#" else s.substring(0, 1).uppercase()
    }
}
