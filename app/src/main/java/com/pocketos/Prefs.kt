package com.pocketos

import android.content.Context

class Prefs(c: Context) {
    private val sp = c.getSharedPreferences("pocketos", Context.MODE_PRIVATE)

    var pack: String
        get() = sp.getString("pack", "ios") ?: "ios"
        set(v) { sp.edit().putString("pack", v).apply() }

    fun webApps(): List<Pair<String, String>> =
        (sp.getString("web", "") ?: "").split("\n").filter { it.contains("|") }.map {
            val i = it.indexOf('|')
            it.substring(0, i) to it.substring(i + 1)
        }

    private fun saveWeb(l: List<Pair<String, String>>) {
        sp.edit().putString("web", l.joinToString("\n") { it.first.replace("|", " ").replace("\n", " ") + "|" + it.second }).apply()
    }

    fun addWeb(title: String, url: String) = saveWeb(webApps().filter { it.second != url } + (title to url))
    fun removeWeb(url: String) = saveWeb(webApps().filter { it.second != url })
}
