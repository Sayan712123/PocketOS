package com.miuidialer

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService

/**
 * Telecom in-call service. When this app holds the default-dialer role, Android hands
 * every incoming / outgoing call to this service, and we render the MIUI call screen.
 */
class MiuInCallService : InCallService() {

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            CallBus.publish()
        }

        override fun onDetailsChanged(call: Call, details: Call.Details) {
            CallBus.publish()
        }
    }

    override fun onCreate() {
        super.onCreate()
        CallBus.service = this
    }

    override fun onDestroy() {
        CallBus.service = null
        super.onDestroy()
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallBus.call?.unregisterCallback(callback)
        CallBus.call = call
        call.registerCallback(callback)
        CallBus.publish()
        showUi()
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callback)
        if (CallBus.call === call) CallBus.call = null
        CallBus.publish()
    }

    private fun showUi() {
        try {
            startActivity(
                Intent(this, CallActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
        } catch (e: Exception) { }
    }
}

/** Tiny bridge between the Telecom service and the call UI. */
object CallBus {
    var service: MiuInCallService? = null
    var call: Call? = null

    private val listeners = ArrayList<() -> Unit>()

    fun add(l: () -> Unit) { if (!listeners.contains(l)) listeners.add(l) }
    fun remove(l: () -> Unit) { listeners.remove(l) }
    fun publish() { listeners.toList().forEach { runCatching { it() } } }
}
