package com.example.smartmemoryapp;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;

public class MainActivity extends Activity {

    private EditText taskInput;
    private TextView voiceStatus, tasksHeader;
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
        tasksHeader = findViewById(R.id.tasksHeader);

        if(findViewById(R.id.btnMicMain) != null) {
            findViewById(R.id.btnMicMain).setOnClickListener(v -> startVoiceInput());
        }

        // --- ربط الأزرار الجديدة ---
        setupTimeButton(R.id.btn15m, 15);
        setupTimeButton(R.id.btn30m, 30);
        setupTimeButton(R.id.btn1h, 60);
        setupTimeButton(R.id.btn2h, 120);
        setupTimeButton(R.id.btn4h, 240);
        
        // زر حفظ بدون منبه
        findViewById(R.id.btnNoAlarm).setOnClickListener(v -> saveTaskQuick(0));
        
        // زر نهاية اليوم (الساعة 10 مساءً)
        findViewById(R.id.btnEndOfDay).setOnClickListener(v -> setEndOfDayAlarm());

        // زر وقت وتكرار (مخصص)
        findViewById(R.id.btnCustom).setOnClickListener(v -> showCustomDateTimePicker());

        loadSavedTasks();
    }

    // دالة مساعدة لضبط الأزرار السريعة
    private void setupTimeButton(int btnId, int minutes) {
        Button btn = findViewById(btnId);
        if (btn != null) {
            btn.setOnClickListener(v -> saveTaskQuick(minutes));
        }
    }

    private void saveTaskQuick(int minutes) {
        String task = taskInput.getText().toString();
        if (task.isEmpty()) task = "مهمة سريعة";
        
        long triggerTime = 0;
        if (minutes > 0) {
            triggerTime = System.currentTimeMillis() + (minutes * 60 * 1000);
            scheduleAlarm(task, triggerTime);
        }
        
        saveTaskToMemory(task, triggerTime, false);
        addVisualTask(task, triggerTime, System.currentTimeMillis(), false);
        
        if (minutes > 0) {
            Toast.makeText(this, "تم ضبط المنبه: " + minutes + " دقيقة", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "تم الحفظ (بدون منبه)", Toast.LENGTH_SHORT).show();
        }
        taskInput.setText("");
    }

    private void setEndOfDayAlarm() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 22); // الساعة 10 مساءً
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        
        if (cal.getTimeInMillis() < System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1); // لو الوقت عدى، خليه بكرة
        }
        
        long diff = cal.getTimeInMillis() - System.currentTimeMillis();
        saveTaskQuick((int)(diff / 60000));
    }

    private void showCustomDateTimePicker() {
        Calendar cal = Calendar.getInstance();
        // 1. اختيار التاريخ
        new DatePickerDialog(this, (view, year, month, day) -> {
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month);
            cal.set(Calendar.DAY_OF_MONTH, day);
            
            // 2. اختيار الوقت
            new TimePickerDialog(this, (tView, hour, minute) -> {
                cal.set(Calendar.HOUR_OF_DAY, hour);
                cal.set(Calendar.MINUTE, minute);
                cal.set(Calendar.SECOND, 0);
                
                long triggerTime = cal.getTimeInMillis();
                if (triggerTime < System.currentTimeMillis()) {
                    Toast.makeText(this, "لا يمكن اختيار وقت في الماضي!", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                String task = taskInput.getText().toString();
                if (task.isEmpty()) task = "مهمة مخصصة";
                
                scheduleAlarm(task, triggerTime);
                saveTaskToMemory(task, triggerTime, false);
                addVisualTask(task, triggerTime, System.currentTimeMillis(), false);
                taskInput.setText("");
                Toast.makeText(this, "تم الجدولة بنجاح 📅", Toast.LENGTH_SHORT).show();
                
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show();
            
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    // --- بناء شكل المهمة (تصميم البطاقة) ---
    private void addVisualTask(String title, long triggerTime, long creationTime, boolean isDone) {
        if (tasksContainer == null) return;
        
        // الكارت الرئيسي
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setPadding(30, 30, 30, 30);
        item.setBackgroundColor(0xFF1E293B); 
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 20);
        item.setLayoutParams(params);
        item.setGravity(Gravity.CENTER_VERTICAL);

        // زر "تم" (الدائرة)
        TextView checkBtn = new TextView(this);
        checkBtn.setText(isDone ? "✅" : "⭕");
        checkBtn.setTextSize(24);
        checkBtn.setPadding(0, 0, 20, 0);
        checkBtn.setOnClickListener(v -> {
            boolean newState = !isDone; // عكس الحالة
            updateTaskStatus(title, triggerTime, creationTime, newState);
            // إعادة رسم القائمة
            tasksContainer.removeView(item);
            addVisualTask(title, triggerTime, creationTime, newState);
        });

        // النصوص (العنوان والوقت)
        LinearLayout textContainer = new LinearLayout(this);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        textContainer.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        
        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextSize(18);
        titleView.setTextColor(isDone ? 0xFF64748B : 0xFFFFFFFF);
        if (isDone) titleView.setPaintFlags(titleView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        
        // حساب النصوص الزمنية
        TextView timeView = new TextView(this);
        String createdText = android.text.format.DateFormat.format("hh:mm a", creationTime).toString();
        String timeInfo = "📝 سُجلت: " + createdText;
        
        if (triggerTime > 0 && !isDone) {
            long diff = triggerTime - System.currentTimeMillis();
            if (diff > 0) {
                long minutesLeft = diff / 60000;
                long hoursLeft = minutesLeft / 60;
                minutesLeft = minutesLeft % 60;
                timeInfo += "\n⏳ باقي: " + hoursLeft + "س " + minutesLeft + "د";
                timeView.setTextColor(0xFFF59E0B); // برتقالي للعد التنازلي
            } else {
                timeInfo += "\n🔔 حان الموعد";
                timeView.setTextColor(0xFFEF4444); // أحمر
            }
        } else if (isDone) {
            timeInfo = "تم الإنجاز ✅";
            timeView.setTextColor(0xFF10B981); // أخضر
        }
        
        timeView.setText(timeInfo);
        timeView.setTextSize(13);
        if (!isDone && triggerTime == 0) timeView.setTextColor(0xFF94A3B8); // رمادي اذا بدون منبه
        
        textContainer.addView(titleView);
        textContainer.addView(timeView);

        // زر الحذف (سلة المهملات)
        TextView deleteBtn = new TextView(this);
        deleteBtn.setText("🗑️");
        deleteBtn.setTextSize(20);
        deleteBtn.setPadding(20, 0, 0, 0);
        deleteBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("حذف المهمة؟")
                .setPositiveButton("نعم", (d, w) -> {
                    removeTaskFromMemory(title);
                    cancelAlarm(title);
                    tasksContainer.removeView(item);
                    updateCount();
                })
                .setNegativeButton("لا", null)
                .show();
        });

        item.addView(checkBtn);
        item.addView(textContainer);
        item.addView(deleteBtn);
        
        // إضافة الكارت في القائمة
        if (isDone) {
            tasksContainer.addView(item); // المنجز في الأسفل
        } else {
            tasksContainer.addView(item, 0); // الجديد في الأعلى
        }
        updateCount();
    }

    private void updateCount() {
        int count = tasksContainer.getChildCount();
        if(tasksHeader != null) tasksHeader.setText("المهام (" + count + ")");
    }

    // --- إدارة التخزين (تم تحديثها لتشمل وقت الإنشاء والحالة) ---
    // المفتاح الآن: Title_DATA
    // القيمة: TriggerTime#CreationTime#IsDone (سلسلة مفصولة)
    
    private void saveTaskToMemory(String task, long triggerTime, boolean isDone) {
        long creationTime = System.currentTimeMillis();
        // لو المهمة موجودة، نحافظ على وقت إنشائها الأصلي
        if (prefs.contains(task + "_DATA")) {
            String existing = prefs.getString(task + "_DATA", "");
            String[] parts = existing.split("#");
            if (parts.length >= 2) creationTime = Long.parseLong(parts[1]);
        }
        
        SharedPreferences.Editor editor = prefs.edit();
        String data = triggerTime + "#" + creationTime + "#" + (isDone ? "1" : "0");
        editor.putString(task + "_DATA", data);
        editor.apply();
    }
    
    private void updateTaskStatus(String task, long triggerTime, long creationTime, boolean isDone) {
        SharedPreferences.Editor editor = prefs.edit();
        String data = triggerTime + "#" + creationTime + "#" + (isDone ? "1" : "0");
        editor.putString(task + "_DATA", data);
        editor.apply();
        
        if (isDone) cancelAlarm(task); // لو تمت، نلغي المنبه
    }

    private void removeTaskFromMemory(String task) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(task + "_DATA");
        editor.apply();
    }

    private void loadSavedTasks() {
        tasksContainer.removeAllViews();
        Map<String, ?> allEntries = prefs.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String key = entry.getKey();
            if (key.endsWith("_DATA")) {
                String taskName = key.replace("_DATA", "");
                String value = (String) entry.getValue();
                String[] parts = value.split("#");
                
                if (parts.length >= 3) {
                    long trigger = Long.parseLong(parts[0]);
                    long created = Long.parseLong(parts[1]);
                    boolean done = parts[2].equals("1");
                    
                    // تنظيف المهام القديمة جداً (أكثر من يومين مثلاً) إذا كانت منتهية
                    // لكن المستخدم يريد الاحتفاظ بها، لذا سنعرضها كلها
                    addVisualTask(taskName, trigger, created, done);
                }
            }
        }
    }

    // --- المنبه والصوت والتصاريح (كما هي) ---
    private void scheduleAlarm(String task, long triggerTime) {
        try {
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(this, AlarmReceiver.class);
            intent.putExtra("task_name", task);
            PendingIntent pi = PendingIntent.getBroadcast(this, task.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE);
            if (am != null) am.setAlarmClock(new AlarmManager.AlarmClockInfo(triggerTime, pi), pi);
        } catch (Exception e) {}
    }

    private void cancelAlarm(String task) {
        try {
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(this, AlarmReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(this, task.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_NO_CREATE);
            if (pi != null && am != null) { am.cancel(pi); pi.cancel(); }
        } catch (Exception e) {}
    }

    private void startVoiceInput() {
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-EG");
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث الآن...");
            startActivityForResult(intent, 10);
        } catch (Exception e) {}
    }

    private void checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 123);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 10 && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                taskInput.setText(result.get(0));
                voiceStatus.setText("تم!");
            }
        }
    }
}
