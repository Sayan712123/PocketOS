package com.miuidialer

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * MIUI / POCO style Phone app: Recents, Contacts and Dialpad tabs plus the
 * default-dialer opt-in that unlocks the MIUI call screen.
 */
class MainActivity : Activity() {

    private var tab = 0
    private val num = StringBuilder()
    private var query = ""

    private lateinit var content: LinearLayout
    private lateinit var tabBar: LinearLayout
    private var listHolder: LinearLayout? = null

    private var numDisplay: TextView? = null
    private var hintView: TextView? = null

    private var contactCache: List<Phonebook.Contact>? = null
    private var pendingNumber: String? = null

    private val perms = arrayOf(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_PHONE_STATE
    )

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        lightBars()
        buildShell()
        askPerms()
        showTab(0)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        contactCache = null
        if (tab == 0 || tab == 1) showTab(tab)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(i: Intent?) {
        val data = i?.data ?: return
        if (data.scheme != "tel") return
        val n = data.schemeSpecificPart.orEmpty().trim()
        if (n.isEmpty()) return
        num.setLength(0)
        num.append(n)
        showTab(2)
        refreshDialpad()
    }

    private fun askPerms() {
        val missing = perms.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) requestPermissions(missing.toTypedArray(), 100)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        contactCache = null
        if (requestCode == 101) {
            val n = pendingNumber
            pendingNumber = null
            if (n != null && checkSelfPermission(Manifest.permission.CALL_PHONE) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                placeCall(n)
                return
            }
        }
        showTab(tab)
    }

    // ---------------------------------------------------------------- shell

    private fun buildShell() {
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(M.BG)

        content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        root.addView(content, lp(MATCH, 0, 1f))

        tabBar = LinearLayout(this)
        tabBar.gravity = Gravity.CENTER
        tabBar.setBackgroundColor(M.BG)
        root.addView(tabBar, lp(MATCH, WRAP))

        setContentView(root)
    }

    private fun showTab(i: Int) {
        tab = i
        content.removeAllViews()
        numDisplay = null
        hintView = null
        listHolder = null
        when (i) {
            0 -> renderRecents()
            1 -> renderContacts()
            else -> renderDialpad()
        }
        buildTabBar()
    }

    private fun buildTabBar() {
        tabBar.removeAllViews()
        val items = listOf(
            Triple("Recents", R.drawable.ic_tab_recents, 0),
            Triple("Contacts", R.drawable.ic_tab_contacts, 1),
            Triple("Dialpad", R.drawable.ic_tab_dialpad, 2)
        )
        items.forEach { (label, icon, idx) ->
            val col = LinearLayout(this)
            col.orientation = LinearLayout.VERTICAL
            col.gravity = Gravity.CENTER_HORIZONTAL
            col.setPadding(0, dp(10), 0, dp(10))
            val iv = ImageView(this)
            iv.setImageResource(icon)
            iv.setColorFilter(if (idx == tab) M.BLUE else M.SUB)
            col.addView(iv, lp(dp(24), dp(24)))
            val t = tv(label, 11f, if (idx == tab) M.BLUE else M.SUB, idx == tab)
            t.gravity = Gravity.CENTER
            col.addView(t, lp(WRAP, WRAP, 0f, 0, dp(4), 0, 0))
            col.setOnClickListener { showTab(idx) }
            tabBar.addView(col, lp(0, WRAP, 1f))
        }
    }

    // ---------------------------------------------------------------- header

    private fun header(title: String, withSearch: Boolean) {
        val bar = LinearLayout(this)
        bar.gravity = Gravity.CENTER_VERTICAL
        bar.setPadding(dp(20), dp(18), dp(20), dp(10))
        bar.addView(tv(title, 26f, M.TEXT, true), lp(0, WRAP, 1f))
        if (withSearch) {
            val iv = ImageView(this)
            iv.setImageResource(R.drawable.ic_search)
            bar.addView(iv, lp(dp(24), dp(24)))
        }
        content.addView(bar, lp(MATCH, WRAP))

        if (withSearch) {
            val box = LinearLayout(this)
            box.gravity = Gravity.CENTER_VERTICAL
            box.background = shape(M.KEY, dp(20).toFloat())
            box.setPadding(dp(14), 0, dp(14), 0)

            val et = EditText(this)
            et.hint = "Search"
            et.setSingleLine()
            et.background = null
            et.inputType = InputType.TYPE_CLASS_TEXT
            et.setText(query)
            et.setSelection(et.text.length)
            et.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    query = s?.toString().orEmpty()
                    when (tab) {
                        0 -> fillRecents()
                        1 -> fillContacts()
                    }
                }
            })
            box.addView(et, lp(0, dp(44), 1f))
            content.addView(box, lp(MATCH, WRAP, 0f, dp(20), 0, dp(20), dp(8)))
        }
    }

    private fun defaultDialerBanner() {
        if (isDefaultDialer()) return

        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.background = shape(0xFFEAF2FF.toInt(), dp(16).toFloat())
        card.setPadding(dp(16), dp(14), dp(16), dp(14))
        card.addView(tv("Set as default Phone app", 15f, M.TEXT, true))
        card.addView(
            tv(
                "The MIUI call screen only appears for real calls when this app is the default dialer.",
                12f, M.SUB
            ),
            lp(MATCH, WRAP, 0f, 0, dp(4), 0, dp(10))
        )

        val btn = tv("Set default", 14f, M.WHITE, true)
        btn.gravity = Gravity.CENTER
        btn.setPadding(dp(18), dp(10), dp(18), dp(10))
        btn.background = shape(M.BLUE, dp(20).toFloat())
        btn.setOnClickListener { requestDialerRole() }
        card.addView(btn, lp(WRAP, WRAP))

        content.addView(card, lp(MATCH, WRAP, 0f, dp(20), dp(4), dp(20), dp(8)))
    }

    private fun isDefaultDialer(): Boolean = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = getSystemService(RoleManager::class.java)
            rm != null && rm.isRoleHeld(RoleManager.ROLE_DIALER)
        } else {
            val tm = getSystemService(TelecomManager::class.java)
            tm?.defaultDialerPackage == packageName
        }
    } catch (e: Exception) {
        false
    }

    private fun requestDialerRole() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val rm = getSystemService(RoleManager::class.java)
                if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                    startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_DIALER), 200)
                    return
                }
            }
            startActivity(
                Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                    .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
            )
        } catch (e: Exception) {
            toast("Open Settings \u2192 Apps \u2192 Default apps \u2192 Phone")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 200) showTab(tab)
    }

    // ---------------------------------------------------------------- recents

    private fun renderRecents() {
        header("Recents", true)
        defaultDialerBanner()

        listHolder = LinearLayout(this)
        listHolder!!.orientation = LinearLayout.VERTICAL
        val sv = ScrollView(this)
        sv.isVerticalScrollBarEnabled = false
        sv.addView(listHolder)
        content.addView(sv, lp(MATCH, 0, 1f))

        fillRecents()
    }

    private fun fillRecents() {
        val holder = listHolder ?: return
        holder.removeAllViews()

        val list = CallLogRepo.recent(this).filter {
            query.isBlank() ||
                (it.name ?: "").contains(query, true) ||
                it.number.contains(query, true)
        }

        if (list.isEmpty()) {
            holder.addView(emptyState("No recent calls"), lp(MATCH, WRAP, 0f, 0, dp(60), 0, 0))
            return
        }

        list.forEach { e -> holder.addView(recentsRow(e), lp(MATCH, WRAP)) }
    }

    private fun recentsRow(e: CallLogRepo.Entry): View {
        val row = LinearLayout(this)
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(dp(20), dp(12), dp(16), dp(12))

        val arrow = tv(CallLogRepo.arrow(e.type), 16f, CallLogRepo.color(e.type), true)
        arrow.gravity = Gravity.CENTER
        arrow.background = circle(0x14FFFFFF or (CallLogRepo.color(e.type) and 0x00FFFFFF))
        row.addView(arrow, lp(dp(26), dp(26)))

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.addView(tv(e.name ?: e.number, 16f, M.TEXT, true), lp(MATCH, WRAP))

        val sub = buildString {
            if (e.name != null) append(e.number)
            val d = CallLogRepo.durationText(e.duration)
            if (d.isNotEmpty()) {
                if (isNotEmpty()) append(" \u00b7 ")
                append(d)
            }
        }
        if (sub.isNotEmpty()) {
            col.addView(tv(sub, 12f, M.SUB), lp(MATCH, WRAP, 0f, 0, dp(2), 0, 0))
        }
        row.addView(col, lp(0, WRAP, 1f, dp(12), 0, dp(8), 0))

        row.addView(
            tv(CallLogRepo.whenText(e.date), 12f, M.SUB),
            lp(WRAP, WRAP, 0f, 0, 0, dp(10), 0)
        )

        val call = ImageView(this)
        call.setImageResource(R.drawable.ic_phone)
        call.setColorFilter(M.BLUE)
        call.setPadding(dp(8), dp(8), dp(8), dp(8))
        call.setOnClickListener { placeCall(e.number) }
        row.addView(call, lp(dp(40), dp(40)))

        row.setOnClickListener { placeCall(e.number) }
        return row
    }

    // ---------------------------------------------------------------- contacts

    private fun renderContacts() {
        header("Contacts", true)

        listHolder = LinearLayout(this)
        listHolder!!.orientation = LinearLayout.VERTICAL
        val sv = ScrollView(this)
        sv.isVerticalScrollBarEnabled = false
        sv.addView(listHolder)
        content.addView(sv, lp(MATCH, 0, 1f))

        fillContacts()
    }

    private fun fillContacts() {
        val holder = listHolder ?: return
        holder.removeAllViews()

        val all = contactCache ?: Phonebook.contacts(this).also { contactCache = it }
        val list = all.filter {
            query.isBlank() ||
                it.name.contains(query, true) ||
                it.number.contains(query, true)
        }

        if (list.isEmpty()) {
            holder.addView(emptyState("No contacts"), lp(MATCH, WRAP, 0f, 0, dp(60), 0, 0))
            return
        }

        list.forEach { c -> holder.addView(contactRow(c), lp(MATCH, WRAP)) }
    }

    private fun contactRow(c: Phonebook.Contact): View {
        val row = LinearLayout(this)
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(dp(20), dp(10), dp(16), dp(10))

        val av = tv(Phonebook.initial(c.name), 18f, M.WHITE, true)
        av.gravity = Gravity.CENTER
        av.background = circle(avatarColor(c.name))
        row.addView(av, lp(dp(44), dp(44)))

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.addView(tv(c.name, 16f, M.TEXT, true), lp(MATCH, WRAP))
        col.addView(tv(c.number, 12f, M.SUB), lp(MATCH, WRAP, 0f, 0, dp(2), 0, 0))
        row.addView(col, lp(0, WRAP, 1f, dp(14), 0, dp(8), 0))

        val call = ImageView(this)
        call.setImageResource(R.drawable.ic_phone)
        call.setColorFilter(M.BLUE)
        call.setPadding(dp(8), dp(8), dp(8), dp(8))
        call.setOnClickListener { placeCall(c.number) }
        row.addView(call, lp(dp(40), dp(40)))

        row.setOnClickListener { placeCall(c.number) }
        return row
    }

    private fun avatarColor(name: String): Int {
        val palette = intArrayOf(
            0xFF3482FF.toInt(), 0xFF12B76A.toInt(), 0xFFF79009.toInt(),
            0xFF7A5AF8.toInt(), 0xFFEE46BC.toInt(), 0xFF06AED4.toInt()
        )
        return palette[Math.abs(name.hashCode()) % palette.size]
    }

    // ---------------------------------------------------------------- dialpad

    private fun renderDialpad() {
        val top = LinearLayout(this)
        top.orientation = LinearLayout.VERTICAL
        top.gravity = Gravity.CENTER_HORIZONTAL
        top.setPadding(dp(24), dp(28), dp(24), 0)

        val d = tv(num.toString(), 34f, M.TEXT)
        d.gravity = Gravity.CENTER
        d.maxLines = 1
        numDisplay = d
        top.addView(d, lp(MATCH, WRAP))

        val h = tv(matchHint(), 13f, M.SUB)
        h.gravity = Gravity.CENTER
        h.maxLines = 1
        hintView = h
        top.addView(h, lp(MATCH, WRAP, 0f, 0, dp(6), 0, 0))

        content.addView(top, lp(MATCH, WRAP))

        val pad = LinearLayout(this)
        pad.orientation = LinearLayout.VERTICAL
        pad.setPadding(dp(28), dp(18), dp(28), 0)

        val keys = listOf(
            "1" to "", "2" to "ABC", "3" to "DEF",
            "4" to "GHI", "5" to "JKL", "6" to "MNO",
            "7" to "PQRS", "8" to "TUV", "9" to "WXYZ",
            "*" to "", "0" to "+", "#" to ""
        )

        for (r in 0..3) {
            val row = LinearLayout(this)
            row.gravity = Gravity.CENTER
            for (c in 0..2) {
                val k = keys[r * 3 + c]
                row.addView(dialKey(k.first, k.second), lp(0, dp(78), 1f))
            }
            pad.addView(row, lp(MATCH, WRAP))
        }
        content.addView(pad, lp(MATCH, WRAP))

        val bottom = LinearLayout(this)
        bottom.gravity = Gravity.CENTER
        bottom.setPadding(dp(28), dp(8), dp(28), dp(16))

        bottom.addView(View(this), lp(0, dp(64), 1f))

        val callBtn = ImageView(this)
        callBtn.setImageResource(R.drawable.ic_phone)
        callBtn.setColorFilter(M.WHITE)
        callBtn.setPadding(dp(18), dp(18), dp(18), dp(18))
        callBtn.background = circle(M.GREEN)
        callBtn.setOnClickListener { placeCall(num.toString()) }
        bottom.addView(callBtn, lp(dp(64), dp(64)))

        val del = ImageView(this)
        del.setImageResource(R.drawable.ic_backspace)
        del.setPadding(dp(16), dp(16), dp(16), dp(16))
        del.setOnClickListener { backspace() }
        del.setOnLongClickListener {
            num.setLength(0)
            refreshDialpad()
            true
        }
        bottom.addView(del, lp(0, dp(64), 1f))

        content.addView(bottom, lp(MATCH, WRAP))
    }

    private fun dialKey(digit: String, letters: String): View {
        val holder = LinearLayout(this)
        holder.gravity = Gravity.CENTER

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.gravity = Gravity.CENTER
        col.background = circle(M.KEY)

        val d = tv(digit, 26f, M.TEXT)
        d.gravity = Gravity.CENTER
        col.addView(d, lp(WRAP, WRAP))

        if (letters.isNotEmpty()) {
            val l = tv(letters, 9f, M.SUB, true)
            l.gravity = Gravity.CENTER
            l.letterSpacing = 0.12f
            col.addView(l, lp(WRAP, WRAP))
        }

        holder.addView(col, lp(dp(66), dp(66)))
        holder.setOnClickListener {
            num.append(digit)
            refreshDialpad()
        }
        holder.setOnLongClickListener {
            if (digit == "0") {
                num.append("+")
                refreshDialpad()
            }
            true
        }
        return holder
    }

    private fun backspace() {
        if (num.isNotEmpty()) num.deleteCharAt(num.length - 1)
        refreshDialpad()
    }

    private fun refreshDialpad() {
        numDisplay?.text = num.toString()
        hintView?.text = matchHint()
    }

    private fun matchHint(): String {
        val d = Phonebook.digits(num.toString())
        if (d.length < 3) return ""
        val list = contactCache ?: Phonebook.contacts(this).also { contactCache = it }
        return list.firstOrNull { Phonebook.digits(it.number) == d }?.name ?: ""
    }

    // ---------------------------------------------------------------- misc

    private fun emptyState(msg: String): View {
        val t = tv(msg, 14f, M.SUB)
        t.gravity = Gravity.CENTER
        return t
    }

    private fun placeCall(number: String) {
        val n = number.trim()
        if (n.isEmpty()) return
        val uri = Uri.parse("tel:" + Uri.encode(n))
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            try {
                startActivity(Intent(Intent.ACTION_CALL, uri))
            } catch (e: Exception) {
                toast("Cannot place call")
            }
        } else {
            pendingNumber = n
            requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), 101)
        }
    }
}
