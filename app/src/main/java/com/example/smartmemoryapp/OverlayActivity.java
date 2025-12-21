package com.example.smartmemoryapp;

import android.app.Activity;
import android.app.AlarmManager;
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
        
        // إعدادات الشاشة لتفتح فوق القفل
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

        // تشغيل الصوت والاهتزاز
        startAlarmSound();

        Button btnDismiss = findViewById(R.id.btnDismiss);
        if(btnDismiss != null) btnDismiss.setOnClickListener(v -> stopAndExit());
        
        setupSnoozeButtons();
    }
    
    private void setupSnoozeButtons() {
        Button btnSnooze = findViewById(R.id.btnSnooze);
        if(btnSnooze != null) {
            btnSnooze.setText("غفوة 5 دقائق 💤");
            btnSnooze.setOnClickListener(v -> snoozeAlarm(5));
        }
    }

    private void startAlarmSound() {
        try {
            // 1. رفع الصوت للحد الأقصى (لضمان السماع)
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 
                                             audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM), 
                                             0);
            }

            // 2. البحث عن نغمة (منبه -> رنين -> إشعارات)
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (soundUri == null) {
                soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                if (soundUri == null) {
                    soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                }
            }

            // 3. تشغيل المشغل
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(getApplicationContext(), soundUri);
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            mediaPlayer.setLooping(true); // تكرار لا نهائي
            mediaPlayer.prepare();
            mediaPlayer.start();

            // 4. الاهتزاز
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                long[] pattern = {0, 1000, 1000}; // اهتزاز ثانية، توقف ثانية
                vibrator.vibrate(pattern, 0);
            }

        } catch (Exception e) {
            // في حالة فشل كل شيء، حاول استخدام ToneGenerator كحل أخير (صوت بيب)
            try {
                android.media.ToneGenerator toneG = new android.media.ToneGenerator(AudioManager.STREAM_ALARM, 100);
                toneG.startTone(android.media.ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 20000); 
            } catch (Exception ex) { }
            e.printStackTrace();
        }
    }

    private void snoozeAlarm(int minutes) {
        stopAlarm();
        
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("task_name", taskName);
        PendingIntent pi = PendingIntent.getBroadcast(this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_IMMUTABLE);
        
        long triggerTime = System.currentTimeMillis() + (minutes * 60 * 1000);
        if (am != null) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pi);
        }
        
        Toast.makeText(this, "تم تأجيل المنبه " + minutes + " دقيقة", Toast.LENGTH_SHORT).show();
        finishAndRemoveTask();
    }

    private void stopAndExit() {
        stopAlarm();
        finishAndRemoveTask();
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
