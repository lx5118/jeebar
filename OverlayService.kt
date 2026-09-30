package com.lex.jeebar

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import java.util.Calendar
import java.util.TimeZone

class OverlayService : Service() {

    // JEE Main 2027 Session 1 starts 22 Jan 2027, 9:00 AM IST (NTA tentative). Change if needed.
    private val target: Long = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).apply {
        clear(); set(2027, Calendar.JANUARY, 22, 9, 0, 0)
    }.timeInMillis

    private val quotes = listOf(
        "Aaj ka PYQ kal ka rank hai.",
        "Phone band. Organic on.",
        "Har second count karta hai.",
        "Discipline > motivation.",
        "IIT tera wait kar raha hai.",
        "Ek aur mechanism, ek aur mark.",
        "Jo abhi soyega, wo sirf sapne dekhega.",
        "NCERT line by line. Bhool mat.",
        "Tu kar sakta hai. Bas baith ja.",
        "Not now = not ever. Padh.",
        "Consistency hi topper banati hai."
    )

    private lateinit var wm: WindowManager
    private var tv: TextView? = null
    private val handler = Handler(Looper.getMainLooper())
    private var n = 0L

    private val loop = object : Runnable {
        override fun run() {
            val left = target - System.currentTimeMillis()
            val cursor = if (n % 2 == 0L) "_" else " "
            val line1 = if (left <= 0) "> EXAM DAY. GO GET IT. $cursor" else {
                val s = left / 1000
                "> T-MINUS %03dd %02dh %02dm %02ds %s".format(
                    s / 86400, (s % 86400) / 3600, (s % 3600) / 60, s % 60, cursor
                )
            }
            val line2 = "# " + quotes[((n / 10) % quotes.size).toInt()]
            tv?.text = "$line1\n$line2"
            n++
            handler.postDelayed(this, 1000 - System.currentTimeMillis() % 1000)
        }
    }

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopSelf(); return START_NOT_STICKY
        }
        startInForeground()
        if (tv == null) addBar()
        handler.removeCallbacks(loop)
        handler.post(loop)
        return START_STICKY
    }

    private fun addBar() {
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dp = resources.displayMetrics.density
        tv = TextView(this).apply {
            typeface = Typeface.MONOSPACE
            setTextColor(Color.parseColor("#00FF41"))
            setShadowLayer(8f, 0f, 0f, Color.parseColor("#00FF41"))
            setBackgroundColor(Color.parseColor("#E6000000"))
            textSize = 12f
            gravity = Gravity.CENTER_VERTICAL
            setPadding((12 * dp).toInt(), (4 * dp).toInt(), (12 * dp).toInt(), (4 * dp).toInt())
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.BOTTOM }
        wm.addView(tv, lp)
    }

    private fun startInForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("jeebar", "JEE Bar", NotificationManager.IMPORTANCE_MIN)
        )
        val stop = PendingIntent.getService(
            this, 0, Intent(this, OverlayService::class.java).setAction("STOP"),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notif = Notification.Builder(this, "jeebar")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("JEE countdown running")
            .addAction(Notification.Action.Builder(null, "Stop", stop).build())
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else startForeground(1, notif)
    }

    override fun onDestroy() {
        handler.removeCallbacks(loop)
        tv?.let { wm.removeView(it) }
        tv = null
        super.onDestroy()
    }
}
