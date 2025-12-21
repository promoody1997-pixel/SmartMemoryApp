package com.example.smartmemoryapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String taskName = intent.getStringExtra("task_name");

        // بدلاً من فتح الشاشة مباشرة، نقوم بتشغيل خدمة الصوت
        Intent serviceIntent = new Intent(context, RingtoneService.class);
        serviceIntent.putExtra("task_name", taskName);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
