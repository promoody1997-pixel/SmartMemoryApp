package com.smartmemory.app

import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class OverlayActivity : AppCompatActivity() {
    
    private var ringtone: Ringtone? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // إعدادات الظهور فوق التطبيقات وشاشة القفل
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        
        setContentView(R.layout.activity_overlay)
        
        val note = intent.getStringExtra("NOTE")
        findViewById<TextView>(R.id.tvOverlayNote).text = note
        
        playAlarm()

        findViewById<Button>(R.id.btnStop).setOnClickListener {
            stopAlarm()
            finish()
        }
        
        findViewById<Button>(R.id.btnSnooze).setOnClickListener {
            stopAlarm()
            // هنا يمكن إضافة منطق الغفوة بإعادة إرسال AlarmManager
            finish()
        }
    }

    private fun playAlarm() {
        try {
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ringtone = RingtoneManager.getRingtone(applicationContext, notification)
            ringtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAlarm() {
        ringtone?.stop()
    }
    
    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }
}
