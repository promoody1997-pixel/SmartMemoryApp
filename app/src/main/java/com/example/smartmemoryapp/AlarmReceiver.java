package com.example.smartmemoryapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // 1. إيقاظ المعالج فوراً لضمان عمل الكود
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "SmartMemory:AlarmWakeLock");
        wl.acquire(10 * 1000L); // استيقاظ لمدة 10 ثوانٍ لضمان فتح الشاشة

        // 2. فتح شاشة التنبيه
        String taskName = intent.getStringExtra("task_name");
        Intent i = new Intent(context, OverlayActivity.class);
        i.putExtra("task_name", taskName);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(i);

        // 3. تحرير القفل لاحقاً (يتم تلقائياً بعد المهلة، لكن جيد كإجراء احترازي)
        // wl.release(); // لا نقوم بتحريره فوراً لنعطي وقتاً للشاشة لتفتح
    }
}
