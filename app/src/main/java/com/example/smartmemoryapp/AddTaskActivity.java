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
        setContentView(R.layout.activity_add_task);

        taskInput = findViewById(R.id.taskInput);

        // زر التسجيل الصوتي
        findViewById(R.id.btnVoice).setOnClickListener(v -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            startActivityForResult(intent, 10);
        });

        // أزرار الوقت
        findViewById(R.id.time15m).setOnClickListener(v -> setTime(15));
        findViewById(R.id.time30m).setOnClickListener(v -> setTime(30));
        findViewById(R.id.time1h).setOnClickListener(v -> setTime(60));

        // زر الحفظ
        findViewById(R.id.btnSave).setOnClickListener(v -> {
            String task = taskInput.getText().toString();
            if (task.isEmpty() || selectedTimeInMillis == 0) {
                Toast.makeText(this, "حدد المهمة والوقت", Toast.LENGTH_SHORT).show();
                return;
            }
            scheduleAlarm(task, selectedTimeInMillis);
            finish();
        });
    }

    private void setTime(int minutes) {
        selectedTimeInMillis = System.currentTimeMillis() + (minutes * 60 * 1000);
        Toast.makeText(this, "تم ضبط الوقت بعد " + minutes + " دقيقة", Toast.LENGTH_SHORT).show();
    }

    private void scheduleAlarm(String task, long time) {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("task_name", task);
        
        PendingIntent pi = PendingIntent.getBroadcast(this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_IMMUTABLE);
        
        // استخدام setExactAndAllowWhileIdle للدقة القصوى
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi);
        Toast.makeText(this, "تم تفعيل المنبه ✅", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 10 && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            taskInput.setText(result.get(0));
        }
    }
}
