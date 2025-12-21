package com.example.smartmemoryapp;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import android.view.View;

public class MainActivity extends Activity {

    private EditText taskInput;
    private TextView voiceStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // التحقق من صلاحية Overlay
        checkOverlayPermission();

        taskInput = findViewById(R.id.mainTaskInput);
        voiceStatus = findViewById(R.id.voiceStatus);

        // زر الميكروفون
        findViewById(R.id.btnMicMain).setOnClickListener(v -> startVoiceInput());

        // أزرار الوقت الملونة (تبرمجت لتعمل فوراً)
        setupTimeButton(R.id.btn15m, 15);
        setupTimeButton(R.id.btn30m, 30);
        setupTimeButton(R.id.btn1h, 60);
        setupTimeButton(R.id.btn2h, 120);
    }

    private void setupTimeButton(int btnId, int minutes) {
        Button btn = findViewById(btnId);
        if (btn != null) {
            btn.setOnClickListener(v -> {
                String task = taskInput.getText().toString();
                if (task.isEmpty()) {
                    task = "مهمة صوتية/سريعة"; // اسم افتراضي اذا لم يكتب شيئاً
                }
                scheduleAlarm(task, minutes);
                
                // إضافة المهمة للقائمة شكلياً (ليرى المستخدم النتيجة)
                addVisualTask(task, minutes);
                
                Toast.makeText(this, "تم ضبط المنبه بعد " + minutes + " دقيقة", Toast.LENGTH_SHORT).show();
                taskInput.setText(""); // تفريغ الحقل
            });
        }
    }

    // هذه الدالة تضيف شكلاً للمهمة في الأسفل لكي تظهر في القائمة
    private void addVisualTask(String title, int minutes) {
        LinearLayout container = findViewById(R.id.tasksContainer);
        
        // إنشاء الحاوية للمهمة
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setPadding(30, 30, 30, 30);
        item.setBackgroundColor(0xFF1E293B); // نفس لون الخلفية في التصميم
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 20);
        item.setLayoutParams(params);

        // النصوص
        LinearLayout textContainer = new LinearLayout(this);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        textContainer.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        
        TextView titleView = new TextView(this);
        titleView.text = title;
        titleView.setTextSize(18);
        titleView.setTextColor(0xFFFFFFFF);
        
        TextView timeView = new TextView(this);
        timeView.text = "⏰ تذكير بعد " + minutes + " دقيقة";
        timeView.setTextColor(0xFFF59E0B); // لون برتقالي
        
        textContainer.addView(titleView);
        textContainer.addView(timeView);
        item.addView(textContainer);
        
        container.addView(item, 0); // إضافة في الأعلى
    }

    private void startVoiceInput() {
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث لتسجيل المهمة...");
            startActivityForResult(intent, 10);
            voiceStatus.setText("جاري الاستماع...");
        } catch (Exception e) {
            Toast.makeText(this, "التسجيل غير مدعوم", Toast.LENGTH_SHORT).show();
        }
    }

    private void scheduleAlarm(String task, int minutes) {
        try {
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(this, AlarmReceiver.class);
            intent.putExtra("task_name", task);
            PendingIntent pi = PendingIntent.getBroadcast(this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_IMMUTABLE);
            
            long triggerTime = System.currentTimeMillis() + (minutes * 60 * 1000);
            if (am != null) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pi);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 123);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 10 && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                taskInput.setText(result.get(0));
                voiceStatus.setText("تم التقاط النص!");
            }
        }
    }
}
