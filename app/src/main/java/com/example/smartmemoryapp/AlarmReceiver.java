package com.example.smartmemoryapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

public class AlarmReceiver extends BroadcastReceiver {
    
    private static final String CHANNEL_ID = "ALARM_CHANNEL_ID";

    @Override
    public void onReceive(Context context, Intent intent) {
        String taskName = intent.getStringExtra("task_name");

        // 1. تجهيز نية فتح الشاشة الحمراء
        Intent fullScreenIntent = new Intent(context, OverlayActivity.class);
        fullScreenIntent.putExtra("task_name", taskName);
        fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                context, 
                taskName != null ? taskName.hashCode() : 0, 
                fullScreenIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 2. إنشاء القناة
        createNotificationChannel(context);

        // 3. بناء الإشعار باستخدام الأدوات الأصلية (Notification.Builder)
        // هذا الكود يعمل بدون أي مكتبات خارجية
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder builder = new Notification.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setContentTitle("⏰ تنبيه الذاكرة!")
                    .setContentText(taskName)
                    .setCategory(Notification.CATEGORY_ALARM)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setAutoCancel(true)
                    // الأمر الذي يفتح الشاشة
                    .setFullScreenIntent(fullScreenPendingIntent, true);

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        } else {
            // للهواتف القديمة جداً (احتياطي)، نفتح النشاط مباشرة
            context.startActivity(fullScreenIntent);
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "منبهات الذاكرة",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("قناة التنبيهات القصوى");
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);
            channel.setSound(null, null); // الصوت يأتي من الشاشة نفسها
            
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
