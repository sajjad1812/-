package com.iosexperience.app.island

import android.app.Notification
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.animation.ValueAnimator
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.text.format.DateFormat
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.iosexperience.app.R
import com.iosexperience.app.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Date
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Draws a Dynamic Island-style pill using the "display over other apps" permission.
 * Compact: shows charging state. Tap to expand: battery level and time.
 * Runs as a foreground service only while the user has the module switched on.
 */
class IslandService : Service() {

    private lateinit var wm: WindowManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var root: FrameLayout? = null
    private var label: TextView? = null
    private var params: WindowManager.LayoutParams? = null
    private val background = GradientDrawable()
    private var animator: ValueAnimator? = null

    private var expanded = false
    private var animationsOn = true
    private var charging = false
    private var level = 0
    private var receiverRegistered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) readBattery(intent)
            refreshText()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        startAsForeground()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        try {
            addIsland()
        } catch (e: RuntimeException) {
            stopSelf()
            return
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_TIME_TICK)
        }
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
            ?.let { readBattery(it) }
        receiverRegistered = true
        refreshText()

        val repo = SettingsRepository(applicationContext)
        scope.launch {
            repo.settings.collect { s ->
                if (!s.islandEnabled) {
                    stopSelf()
                } else {
                    animationsOn = s.animations
                    params?.let { p ->
                        p.y = dp(s.islandTopOffset.roundToInt())
                        root?.let { v -> if (v.isAttachedToWindow) wm.updateViewLayout(v, p) }
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY

    override fun onDestroy() {
        scope.cancel()
        animator?.cancel()
        if (receiverRegistered) unregisterReceiver(receiver)
        root?.let { if (it.isAttachedToWindow) wm.removeView(it) }
        root = null
        super.onDestroy()
    }

    // ---- foreground notification

    private fun startAsForeground() {
        NotificationManagerCompat.from(this).createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(getString(R.string.mod_island))
                .build(),
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.island_notification))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    // ---- overlay view

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).roundToInt()

    private fun addIsland() {
        background.setColor(Color.BLACK)
        background.cornerRadius = dp(COMPACT_H) / 2f

        val text = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        val container = FrameLayout(this).apply {
            background = this@IslandService.background
            addView(
                text,
                FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
            )
            setOnClickListener { toggle() }
        }
        val lp = WindowManager.LayoutParams(
            dp(COMPACT_W),
            dp(COMPACT_H),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(8)
        }
        wm.addView(container, lp)
        root = container
        label = text
        params = lp
    }

    private fun toggle() {
        expanded = !expanded
        refreshText()
        if (expanded) animateTo(dp(EXPANDED_W), dp(EXPANDED_H)) else animateTo(dp(COMPACT_W), dp(COMPACT_H))
    }

    private fun animateTo(w: Int, h: Int) {
        val p = params ?: return
        animator?.cancel()
        if (!animationsOn) {
            applySize(w, h)
            return
        }
        val w0 = p.width
        val h0 = p.height
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 350
            interpolator = OvershootInterpolator(1.1f)
            addUpdateListener {
                val f = it.animatedValue as Float
                applySize((w0 + (w - w0) * f).roundToInt(), (h0 + (h - h0) * f).roundToInt())
            }
            start()
        }
    }

    private fun applySize(w: Int, h: Int) {
        val p = params ?: return
        val v = root ?: return
        p.width = max(w, 1)
        p.height = max(h, 1)
        background.cornerRadius = min(p.height / 2f, dp(28).toFloat())
        if (v.isAttachedToWindow) wm.updateViewLayout(v, p)
    }

    // ---- content

    private fun readBattery(intent: Intent) {
        val l = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        if (l >= 0 && scale > 0) level = (l * 100f / scale).roundToInt()
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun refreshText() {
        val tv = label ?: return
        tv.text = if (expanded) {
            val state = getString(if (charging) R.string.island_charging else R.string.island_battery)
            val time = DateFormat.getTimeFormat(this).format(Date())
            "$level%  •  $state\n$time"
        } else if (charging) {
            "\u26A1 $level%"
        } else {
            ""
        }
    }

    private companion object {
        const val CHANNEL_ID = "island"
        const val NOTIFICATION_ID = 1001
        const val COMPACT_W = 120
        const val COMPACT_H = 34
        const val EXPANDED_W = 300
        const val EXPANDED_H = 90
    }
}
