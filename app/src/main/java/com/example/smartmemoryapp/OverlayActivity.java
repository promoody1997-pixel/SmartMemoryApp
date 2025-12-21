package com.example.smartmemoryapp;

import android.app.Activity;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class OverlayActivity extends Activity {
    private Ringtone ringtone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // إعدادات للظهور فوق شاشة القفل وإيقاظ الهاتف
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
                
        setContentView(R.layout.activity_overlay);

        String task = getIntent().getStringExtra("task_name");
        ((TextView) findViewById(R.id.overlayTaskName)).setText(task);

        // تشغيل الصوت
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
        ringtone.play();

        Button btnDismiss = findViewById(R.id.btnDismiss);
        btnDismiss.setOnClickListener(v -> {
            if (ringtone != null) ringtone.stop();
            finish();
        });
        
        // زر الغفوة يغلق الشاشة فقط حالياً (يمكن تطويره لاحقاً)
        findViewById(R.id.btnSnooze).setOnClickListener(v -> {
             if (ringtone != null) ringtone.stop();
             finish();
        });
    }
    
    @Override
    protected void onDestroy() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
        super.onDestroy();
    }
}
