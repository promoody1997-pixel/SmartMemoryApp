package com.example.smartmemoryapp;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class OverlayActivity extends Activity {
    
    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private String taskName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // إعدادات لفتح الشاشة فوق القفل
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
        if(taskText != null && taskName != null) taskText.setText(taskName);

        // تشغيل التنبيه
        startAlarmSound();

        // برمجة زر الإيقاف (يغلق الصوت فقط ولا يحذف المهمة)
        Button btnStop = findViewById(R.id.btnStop);
        btnStop.setOnClickListener(v -> stopAndExit());
        
        // برمجة أزرار الغفوة
        findViewById(R.id.btnSnooze5).setOnClickListener(v -> snoozeAlarm(5));
        findViewById(R.id.btnSnooze15).setOnClickListener(v -> snoozeAlarm(15));
        findViewById(R.id.btnSnooze60).setOnClickListener(v -> snoozeAlarm(60));
    }

    private void startAlarmSound() {
        try {
            // إلغاء أي إشعار سابق في شريط الإشعارات
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancelAll();

            // رفع الصوت
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 
                                             audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM), 
                                             0);
            }

            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (soundUri == null) soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(getApplicationContext(), soundUri);
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();

            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                long[] pattern = {0, 1000, 1000};
                vibrator.vibrate(pattern, 0);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void snoozeAlarm(int minutes) {
        stopAlarm(); // إيقاف الصوت الحالي
        
        // جدولة منبه جديد
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("task_name", taskName);
        
        // استخدام ID فريد للغفوة
        PendingIntent pi = PendingIntent.getBroadcast(this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_IMMUTABLE);
        
        long triggerTime = System.currentTimeMillis() + (minutes * 60 * 1000);
        
        if (am != null) {
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(triggerTime, pi), pi);
        }
        
        Toast.makeText(this, "تمت الغفوة: " + minutes + " دقيقة 💤", Toast.LENGTH_SHORT).show();
        finishAndRemoveTask();
    }

    private void stopAndExit() {
        stopAlarm();
        finishAndRemoveTask(); // يغلق الشاشة ويعود للتطبيق أو الصفحة الرئيسية
    }

    private void stopAlarm() {
        if (mediaPlayer != null) {
            if(mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (vibrator != null) {
            vibrator.cancel();
        }
    }
    
    @Override
    protected void onDestroy() {
        stopAlarm();
        super.onDestroy();
    }
}
