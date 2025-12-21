package com.example.smartmemoryapp;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Map;

public class MainActivity extends Activity {

    private EditText taskInput;
    private TextView voiceStatus;
    private LinearLayout tasksContainer;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("SmartMemoryTasks", MODE_PRIVATE);
        
        checkOverlayPermission();

        taskInput = findViewById(R.id.mainTaskInput);
        voiceStatus = findViewById(R.id.voiceStatus);
        tasksContainer = findViewById(R.id.tasksContainer);

        if(findViewById(R.id.btnMicMain) != null) {
            findViewById(R.id.btnMicMain).setOnClickListener(v -> startVoiceInput());
        }

        // === هنا تمت إضافة زر التجربة ===
        setupTimeButton(R.id.btn1m, 1);   // زر 1 دقيقة
        setupTimeButton(R.id.btn15m, 15);
        setupTimeButton(R.id.btn30m, 30);
        setupTimeButton(R.id.btn1h, 60);
        setupTimeButton(R.id.btn2h, 120);

        loadSavedTasks();
    }

    private void setupTimeButton(int btnId, int minutes) {
        Button btn = findViewById(btnId);
        if (btn != null) {
            btn.setOnClickListener(v -> {
                String task = "";
                if (taskInput != null) task = taskInput.getText().toString();
                if (task.isEmpty()) task = "مهمة تجريبية";
                
                createNewTask(task, minutes);
                if (taskInput != null) taskInput.setText(""); 
            });
        }
    }

    private void createNewTask(String task, int minutes) {
        long triggerTime = System.currentTimeMillis() + (minutes * 60 * 1000);
        
        scheduleAlarm(task, triggerTime);
        saveTaskToMemory(task, triggerTime);
        addVisualTask(task, minutes, triggerTime);
        
        Toast.makeText(this, "تم ضبط المنبه: " + minutes + " دقيقة ⏳", Toast.LENGTH_SHORT).show();
    }

    private void addVisualTask(String title, int minutesLeft, long triggerTime) {
        if (tasksContainer == null) return;
        
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setPadding(40, 40, 40, 40);
        item.setBackgroundColor(0xFF1E293B); 
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
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        
        TextView timeView = new TextView(this);
        String timeText = (minutesLeft > 0) ? "⏰ بعد " + minutesLeft + " دقيقة" : "⏰ مجدول: " + android.text.format.DateFormat.format("hh:mm a", triggerTime);
        timeView.setText(timeText);
        timeView.setTextColor(0xFFF59E0B);
        timeView.setPadding(0, 10, 0, 0);
        
        textContainer.addView(titleView);
        textContainer.addView(timeView);
        item.addView(textContainer);
        
        item.setOnClickListener(v -> showEditDialog(title, triggerTime, item));
        
        tasksContainer.addView(item, 0);
    }

    private void showEditDialog(String currentTitle, long oldTime, View itemView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle("خيارات المهمة");
        
        String[] options = {"تمديد الوقت (+15 دقيقة)", "حذف / تم الانتهاء", "إلغاء"};
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                removeTaskFromMemory(currentTitle);
                createNewTask(currentTitle, 15);
                tasksContainer.removeView(itemView);
            } else if (which == 1) {
                removeTaskFromMemory(currentTitle);
                tasksContainer.removeView(itemView);
                cancelAlarm(currentTitle);
                Toast.makeText(this, "تم حذف المهمة", Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }

    private void saveTaskToMemory(String task, long time) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(task + "_TIME", time);
        editor.apply();
    }

    private void removeTaskFromMemory(String task) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(task + "_TIME");
        editor.apply();
    }

    private void loadSavedTasks() {
        Map<String, ?> allEntries = prefs.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String key = entry.getKey();
            if (key.endsWith("_TIME")) {
                String taskName = key.replace("_TIME", "");
                long time = (Long) entry.getValue();
                long diff = time - System.currentTimeMillis();
                if (diff > 0) {
                    addVisualTask(taskName, (int)(diff / 60000), time);
                } else {
                    removeTaskFromMemory(taskName);
                }
            }
        }
    }

    private void startVoiceInput() {
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-EG");
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث الآن...");
            startActivityForResult(intent, 10);
        } catch (Exception e) {
            Toast.makeText(this, "غير مدعوم", Toast.LENGTH_SHORT).show();
        }
    }

    private void scheduleAlarm(String task, long triggerTime) {
        try {
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(this, AlarmReceiver.class);
            intent.putExtra("task_name", task);
            int uniqueId = task.hashCode();
            PendingIntent pi = PendingIntent.getBroadcast(this, uniqueId, intent, PendingIntent.FLAG_IMMUTABLE);
            
            if (am != null) {
                AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(triggerTime, pi);
                am.setAlarmClock(info, pi);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cancelAlarm(String task) {
        try {
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(this, AlarmReceiver.class);
            int uniqueId = task.hashCode();
            PendingIntent pi = PendingIntent.getBroadcast(this, uniqueId, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_NO_CREATE);
            if (pi != null && am != null) {
                am.cancel(pi);
                pi.cancel();
            }
        } catch (Exception e) {}
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
                if (taskInput != null) taskInput.setText(result.get(0));
                if (voiceStatus != null) voiceStatus.setText("تم!");
            }
        }
    }
}
