package com.example.smartmemoryapp;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class OverlayActivity extends Activity {
    
    private String taskName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // إعدادات فتح الشاشة
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
                
        setContentView(R.layout.activity_overlay);

        taskName = getIntent().getStringExtra("task_name");
        TextView taskText = findViewById(R.id.overlayTaskName);
        if(taskText != null) taskText.setText(taskName);

        // ملاحظة: الصوت يعمل الآن عبر RingtoneService، لا حاجة لتشغيله هنا

        // زر الإيقاف
        findViewById(R.id.btnStop).setOnClickListener(v -> stopAndExit());
        
        // أزرار الغفوة
        findViewById(R.id.btnSnooze5).setOnClickListener(v -> snoozeAlarm(5));
        findViewById(R.id.btnSnooze15).setOnClickListener(v -> snoozeAlarm(15));
        findViewById(R.id.btnSnooze60).setOnClickListener(v -> snoozeAlarm(60));
    }

    private void snoozeAlarm(int minutes) {
        stopService(new Intent(this, RingtoneService.class)); // إيقاف الصوت
        
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("task_name", taskName);
        PendingIntent pi = PendingIntent.getBroadcast(this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_IMMUTABLE);
        
        long triggerTime = System.currentTimeMillis() + (minutes * 60 * 1000);
        if (am != null) {
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(triggerTime, pi), pi);
        }
        
        Toast.makeText(this, "تمت الغفوة: " + minutes + " دقيقة 💤", Toast.LENGTH_SHORT).show();
        finishAndRemoveTask();
    }

    private void stopAndExit() {
        // عند الضغط على إيقاف، نقتل خدمة الصوت
        stopService(new Intent(this, RingtoneService.class));
        finishAndRemoveTask();
    }
    
    @Override
    protected void onDestroy() {
        // ضمان إيقاف الصوت عند إغلاق الشاشة بأي طريقة
        // stopService(new Intent(this, RingtoneService.class)); // يمكن تفعيل هذا السطر لو أردت إيقاف الصوت بمجرد الخروج
        super.onDestroy();
    }
}
