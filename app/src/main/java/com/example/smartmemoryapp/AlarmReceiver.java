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
import androidx.core.app.NotificationCompat;

public class AlarmReceiver extends BroadcastReceiver {
    
    // معرف القناة ثابت
    private static final String CHANNEL_ID = "ALARM_CHANNEL_ID";

    @Override
    public void onReceive(Context context, Intent intent) {
        String taskName = intent.getStringExtra("task_name");

        // 1. تجهيز النية لفتح شاشة التنبيه (Overlay)
        Intent fullScreenIntent = new Intent(context, OverlayActivity.class);
        fullScreenIntent.putExtra("task_name", taskName);
        fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                context, 
                taskName.hashCode(), 
                fullScreenIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 2. إنشاء قناة الإشعارات (مطلوب لأندرويد 8+)
        createNotificationChannel(context);

        // 3. بناء الإشعار "شديد الأهمية"
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // أيقونة صغيرة
                .setContentTitle("⏰ حان وقت المهمة!")
                .setContentText(taskName)
                .setPriority(NotificationCompat.PRIORITY_MAX) // أولوية قصوى
                .setCategory(NotificationCompat.CATEGORY_ALARM) // تصنيف كمنبه
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // يظهر فوق القفل
                .setAutoCancel(true)
                // السطر السحري: هذا ما يجبر الشاشة على الفتح
                .setFullScreenIntent(fullScreenPendingIntent, true);

        // 4. إطلاق الإشعار
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // نستخدم ID فريد بناءً على الوقت لضمان ظهور كل منبه
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        
        // ملاحظة: OverlayActivity سيقوم بتشغيل الصوت فور فتحه
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "تنبيهات المهام",
                    NotificationManager.IMPORTANCE_HIGH // أهمية عالية جداً
            );
            channel.setDescription("قناة خاصة بمنبهات الذاكرة الذكية");
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);
            // الصوت سيعالجه OverlayActivity، لذا نجعله صامتاً في الإشعار لمنع التداخل
            channel.setSound(null, null); 
            
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
