package com.example.smartmemoryapp;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import java.util.ArrayList;

public class AddTaskActivity extends Activity {

    private EditText taskInput;
    private long selectedTimeInMillis = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_add_task);

            taskInput = findViewById(R.id.taskInput);

            // ربط الأزرار مع التأكد من وجودها
            setupButton(R.id.btnVoice, v -> startVoiceInput());
            setupButton(R.id.time15m, v -> setTime(15));
            setupButton(R.id.time30m, v -> setTime(30));
            setupButton(R.id.time1h, v -> setTime(60));
            // الزر اليدوي (مؤقتاً سنعطيه 5 دقائق للتجربة)
            setupButton(R.id.timeCustom, v -> setTime(5)); 

            setupButton(R.id.btnSave, v -> saveTask());

        } catch (Exception e) {
            // في حالة حدوث خطأ، اظهر رسالة بدلاً من إغلاق التطبيق
            Toast.makeText(this, "حدث خطأ في التصميم: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    // دالة مساعدة لربط الأزرار بأمان
    private void setupButton(int id, android.view.View.OnClickListener listener) {
        Button btn = findViewById(id);
        if (btn != null) btn.setOnClickListener(listener);
    }

    private void startVoiceInput() {
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث الآن...");
            startActivityForResult(intent, 10);
        } catch (Exception e) {
            Toast.makeText(this, "التسجيل الصوتي غير مدعوم في هذا الجهاز", Toast.LENGTH_SHORT).show();
        }
    }

    private void setTime(int minutes) {
        selectedTimeInMillis = System.currentTimeMillis() + (minutes * 60 * 1000);
        Toast.makeText(this, "تم اختيار: بعد " + minutes + " دقيقة", Toast.LENGTH_SHORT).show();
    }

    private void saveTask() {
        String task = taskInput.getText().toString();
        if (task.isEmpty()) {
            Toast.makeText(this, "الرجاء كتابة المهمة أولاً", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedTimeInMillis == 0) {
            Toast.makeText(this, "الرجاء اختيار الوقت", Toast.LENGTH_SHORT).show();
            return;
        }
        
        scheduleAlarm(task, selectedTimeInMillis);
        finish();
    }

    private void scheduleAlarm(String task, long time) {
        try {
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(this, AlarmReceiver.class);
            intent.putExtra("task_name", task);
            
            PendingIntent pi = PendingIntent.getBroadcast(this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_IMMUTABLE);
            
            if (am != null) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi);
                Toast.makeText(this, "تم ضبط المنبه بنجاح ✅", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "فشل ضبط المنبه: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 10 && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                taskInput.setText(result.get(0));
            }
        }
    }
}
