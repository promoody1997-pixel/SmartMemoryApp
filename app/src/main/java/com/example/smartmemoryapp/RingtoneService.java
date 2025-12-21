package com.example.smartmemoryapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Vibrator;

public class RingtoneService extends Service {
    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private static final String CHANNEL_ID = "SERVICE_ALARM_CHANNEL_MAX"; // غيرنا الاسم لتحديث الإعدادات

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String taskName = (intent != null && intent.getStringExtra("task_name") != null) ? intent.getStringExtra("task_name") : "تنبيه";

        // 1. إظهار الإشعار فوراً (بأولوية قصوى)
        startForeground(1, buildNotification(taskName));

        // 2. تأخير تشغيل الصوت لمدة 1.5 ثانية (1500 مللي ثانية)
        // هذا يعطي وقتاً للإشعار ليظهر على الشاشة قبل أن يبدأ الإزعاج
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            playAlarm();
        }, 1500);

        return START_NOT_STICKY;
    }

    private Notification buildNotification(String taskName) {
        Intent fullScreenIntent = new Intent(this, OverlayActivity.class);
        fullScreenIntent.putExtra("task_name", taskName);
        fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                this, 0, fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // استخدام IMPORTANCE_MAX بدلاً من HIGH ليظهر فوق أي شيء
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Critical Alarm Service", NotificationManager.IMPORTANCE_MAX);
            channel.setSound(null, null);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);

            return new Notification.Builder(this, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setContentTitle("⏰ " + taskName)
                    .setContentText("اضغط هنا لإيقاف المنبه")
                    .setCategory(Notification.CATEGORY_ALARM)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setPriority(Notification.PRIORITY_MAX) // أولوية قصوى
                    .setFullScreenIntent(fullScreenPendingIntent, true)
                    .build();
        }
        return new Notification();
    }

    private void playAlarm() {
        // نتأكد أن الخدمة لم تتوقف قبل تشغيل الصوت
        try {
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
            if (vibrator != null) vibrator.vibrate(new long[]{0, 1000, 1000}, 0);
            
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) { 
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception e) {}
            mediaPlayer = null;
        }
        if (vibrator != null) vibrator.cancel();
    }
}
