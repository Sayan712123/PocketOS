package com.pocketos

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

class ControlCenterActivity : Activity() {
    companion object { var torchOn = false }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.setBackgroundDrawable(ColorDrawable(0x66000000))
        val root = FrameLayout(this)
        root.setOnClickListener { finish() }
        val sheet = LinearLayout(this)
        sheet.orientation = LinearLayout.VERTICAL
        sheet.isClickable = true
        sheet.setPadding(dp(14), dp(14), dp(14), dp(14))
        sheet.background = shape(0xEE2C2C2E.toInt(), dp(28).toFloat())

        val r1 = LinearLayout(this)
        r1.addView(tile("Wi-Fi") { panel(if (Build.VERSION.SDK_INT >= 29) Settings.Panel.ACTION_INTERNET_CONNECTIVITY else Settings.ACTION_WIFI_SETTINGS) }, tl())
        r1.addView(tile("Bluetooth") { panel(Settings.ACTION_BLUETOOTH_SETTINGS) }, tl())
        r1.addView(tile("Airplane") { panel(Settings.ACTION_AIRPLANE_MODE_SETTINGS) }, tl())
        sheet.addView(r1)

        val r2 = LinearLayout(this)
        r2.addView(tile("Flashlight", torchOn) { t -> toggleTorch(t) }, tl())
        r2.addView(tile("Sound") { panel(Settings.ACTION_SOUND_SETTINGS) }, tl())
        r2.addView(tile("Settings") { panel(Settings.ACTION_SETTINGS) }, tl())
        sheet.addView(r2)

        sheet.addView(tv("Brightness", 13f, Color.WHITE, true), lp(WRAP, WRAP, 0f, dp(6), dp(12), 0, 0))
        val br = SeekBar(this)
        br.max = 255
        br.progress = try { Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS) } catch (e: Exception) { 128 }
        br.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) {
                if (!fromUser) return
                if (Settings.System.canWrite(this@ControlCenterActivity)) {
                    Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                    Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, maxOf(p, 5))
                } else {
                    startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName")))
                    finish()
                }
            }
            override fun onStartTrackingTouch(s: SeekBar) {}
            override fun onStopTrackingTouch(s: SeekBar) {}
        })
        sheet.addView(br)

        sheet.addView(tv("Volume", 13f, Color.WHITE, true), lp(WRAP, WRAP, 0f, dp(6), dp(8), 0, 0))
        val am = getSystemService(AudioManager::class.java)
        val vol = SeekBar(this)
        vol.max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        vol.progress = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        vol.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) {
                if (fromUser) am.setStreamVolume(AudioManager.STREAM_MUSIC, p, 0)
            }
            override fun onStartTrackingTouch(s: SeekBar) {}
            override fun onStopTrackingTouch(s: SeekBar) {}
        })
        sheet.addView(vol)

        root.addView(sheet, FrameLayout.LayoutParams(MATCH, WRAP).apply { setMargins(dp(12), dp(36), dp(12), 0) })
        setContentView(root)
    }

    private fun tl() = lp(0, dp(76), 1f, dp(4), dp(4), dp(4), dp(4))

    private fun tile(label: String, on: Boolean = false, click: (TextView) -> Unit): TextView {
        val t = tv(label, 13f, Color.WHITE, true)
        t.gravity = Gravity.CENTER
        t.background = shape(if (on) C.BLUE else 0xFF3A3A3C.toInt(), dp(18).toFloat())
        t.setOnClickListener { click(t) }
        return t
    }

    private fun panel(action: String) {
        try { startActivity(Intent(action)) } catch (e: Exception) { toast("Not available") }
        finish()
    }

    private fun toggleTorch(t: TextView) {
        try {
            val cm = getSystemService(CameraManager::class.java)
            val id = cm.cameraIdList.firstOrNull {
                cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return
            torchOn = !torchOn
            cm.setTorchMode(id, torchOn)
            t.background = shape(if (torchOn) C.BLUE else 0xFF3A3A3C.toInt(), dp(18).toFloat())
        } catch (e: Exception) {
            toast("Flashlight unavailable")
        }
    }
}
