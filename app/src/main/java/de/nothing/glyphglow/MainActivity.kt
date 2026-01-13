package de.nothing.glyphglow

import android.content.ComponentName
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphException
import com.nothing.ketchum.GlyphFrame
import com.nothing.ketchum.GlyphManager

class MainActivity : AppCompatActivity() {
    private var glyphManager: GlyphManager? = null
    private var isServiceReady: Boolean = false
    private var aOn = false
    private var bOn = false
    private var cOn = false
    private var cProgress = 0 // 0..100 for progress percentage
    private var statusView: TextView? = null

    private val callback = object : GlyphManager.Callback {
        override fun onServiceConnected(componentName: ComponentName) {
            try {
                val gm = glyphManager ?: return
                statusView?.text = "Status: service connected"
                val ok = when {
                    Common.is23113() -> gm.register(Glyph.DEVICE_23113) // Phone (2a) Plus
                    Common.is23111() -> gm.register(Glyph.DEVICE_23111) // Phone (2a)
                    Common.is24111() -> gm.register(Glyph.DEVICE_24111) // Phone (3a/Pro)
                    Common.is22111() -> gm.register(Glyph.DEVICE_22111)
                    Common.is20111() -> gm.register(Glyph.DEVICE_20111)
                    else -> gm.register()
                }
                if (!ok) {
                    Toast.makeText(this@MainActivity, "Glyph register failed", Toast.LENGTH_SHORT).show()
                    statusView?.text = "Status: register failed"
                }
                gm.openSession()
                statusView?.text = "Status: session open"
                isServiceReady = true
                // Reset device view, then apply current UI state
                try { gm.turnOff() } catch (_: Exception) {}
                applyAll()
            } catch (e: GlyphException) {
                Log.e(TAG, "Glyph openSession failed: ${e.message}")
                Toast.makeText(this@MainActivity, "Glyph session error", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            isServiceReady = false
            statusView?.text = "Status: service disconnected"
            try {
                glyphManager?.closeSession()
            } catch (_: Exception) {
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        glyphManager = GlyphManager.getInstance(applicationContext)
        glyphManager?.init(callback)

        statusView = findViewById(R.id.statusText)
        statusView?.text = "Status: binding service..."
        val btnA: SwitchMaterial = findViewById(R.id.btnA)
        val btnB: SwitchMaterial = findViewById(R.id.btnB)
        val sliderC: Slider = findViewById(R.id.sliderC)
        // Configure slider in code to avoid AAPT attr issues
        sliderC.valueFrom = 0f
        sliderC.valueTo = 100f
        sliderC.stepSize = 1f

        btnA.setOnCheckedChangeListener { _, isChecked -> setZoneA(isChecked) }
        btnB.setOnCheckedChangeListener { _, isChecked -> setZoneB(isChecked) }
        sliderC.addOnChangeListener { _, value, fromUser ->
            val newProgress = value.toInt().coerceIn(0, 100)
            val wasOn = cProgress > 0
            val isNowOn = newProgress > 0
            cProgress = newProgress
            if (fromUser && isServiceReady) {
                // Trigger applyAll only if C state changed (on->off or off->on) or if already on
                if (wasOn != isNowOn || isNowOn) {
                    applyAll()
                }
            }
        }
    }

    override fun onDestroy() {
        try {
            glyphManager?.closeSession()
        } catch (_: Exception) {
        }
        glyphManager?.unInit()
        super.onDestroy()
    }

    private fun setZoneA(on: Boolean) {
        aOn = on
        applyAll()
    }

    private fun setZoneB(on: Boolean) {
        bOn = on    
        applyAll()
    }

    private fun applyAll() {
        if (!isServiceReady) return
        val gm = glyphManager ?: return
        try { gm.turnOff() } catch (_: Exception) {}
        
        if (!aOn && !bOn && cProgress == 0) return
        
        val builder: GlyphFrame.Builder = gm.glyphFrameBuilder
        if (aOn) builder.buildChannelA()
        if (bOn) builder.buildChannelB()
        if (cProgress > 0) builder.buildChannelC()
        
        val frame = builder.build()
        
        // Only use displayProgress if C is the only active channel
        if (cProgress > 0 && !aOn && !bOn) {
            gm.displayProgress(frame, cProgress, false)
        } else {
            gm.toggle(frame)
        }
    }

    companion object {
        private const val TAG = "GlyphGlow"
    }
}