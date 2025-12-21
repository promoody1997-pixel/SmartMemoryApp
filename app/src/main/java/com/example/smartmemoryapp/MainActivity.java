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

        // طلب صلاحية الظهور فوق التطبيقات
        checkOverlayPermission();

        // ربط العناصر بالتصميم الجديد
        taskInput = findViewById(R.id.mainTaskInput);
        voiceStatus = findViewById(R.id.voiceStatus);

        // زر الميكروفون
        if(findViewById(R.id.btnMicMain) != null) {
            findViewById(R.id.btnMicMain).setOnClickListener(v -> startVoiceInput());
        }

        // أزرار الوقت الملونة (تعمل فوراً)
        setupTimeButton(R.id.btn15m, 15);
        setupTimeButton(R.id.btn30m, 30);
        setupTimeButton(R.id.btn1h, 60);
        setupTimeButton(R.id.btn2h, 120);
    }

    private void setupTimeButton(int btnId, int minutes) {
        Button btn = findViewById(btnId);
        if (btn != null) {
            btn.setOnClickListener(v -> {
                String task = "";
                if (taskInput != null) {
                    task = taskInput.getText().toString();
                }
                
                if (task.isEmpty()) {
                    task = "مهمة سريعة"; // اسم افتراضي
                }
                
                scheduleAlarm(task, minutes);
                addVisualTask(task, minutes);
                
                Toast.makeText(this, "تم ضبط المنبه بعد " + minutes + " دقيقة", Toast.LENGTH_SHORT).show();
                if (taskInput != null) taskInput.setText(""); // تفريغ الحقل
            });
        }
    }

    // دالة لإضافة المهمة شكلياً في القائمة
    private void addVisualTask(String title, int minutes) {
        LinearLayout container = findViewById(R.id.tasksContainer);
        if (container == null) return;
        
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setPadding(30, 30, 30, 30);
        item.setBackgroundColor(0xFF1E293B); // نفس لون الخلفية الداكنة
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 20);
        item.setLayoutParams(params);

        LinearLayout textContainer = new LinearLayout(this);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        textContainer.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        
        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextSize(18);
        titleView.setTextColor(0xFFFFFFFF);
        
        TextView timeView = new TextView(this);
        timeView.setText("⏰ تذكير بعد " + minutes + " دقيقة");
        timeView.setTextColor(0xFFF59E0B);
        
        textContainer.addView(titleView);
        textContainer.addView(timeView);
        item.addView(textContainer);
        
        container.addView(item, 0);
    }

    // دالة التسجيل الصوتي (مع إجبار اللغة العربية)
    private void startVoiceInput() {
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            
            // ============================================================
            // كود إجبار اللغة العربية (مصر) حتى لو الهاتف إنجليزي
            // ============================================================
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-EG");
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar-EG"); 
            intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "ar-EG");
            
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث الآن بالعربية...");
            startActivityForResult(intent, 10);
            
            if(voiceStatus != null) voiceStatus.setText("جاري الاستماع...");
            
        } catch (Exception e) {
            Toast.makeText(this, "خدمة الصوت غير متوفرة", Toast.LENGTH_SHORT).show();
        }
    }

    private void scheduleAlarm(String task, int minutes) {
        try {
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(this, AlarmReceiver.class);
            intent.putExtra("task_name", task);
            
            // استخدام FLAG_MUTABLE أو IMMUTABLE حسب الحاجة، هنا نستخدم IMMUTABLE للأمان
            PendingIntent pi = PendingIntent.getBroadcast(this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_IMMUTABLE);
            
            long triggerTime = System.currentTimeMillis() + (minutes * 60 * 1000);
            if (am != null) {
                // إجبار المنبه على العمل بدقة حتى في وضع السكون
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pi);
            }
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
                if (taskInput != null) {
                    taskInput.setText(result.get(0));
                }
                if (voiceStatus != null) {
                    voiceStatus.setText("تم الالتقاط: " + result.get(0));
                }
            }
        }
    }
}
