package com.example.smartmemoryapp;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.view.Gravity;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // إنشاء نص ترحيبي بسيط
        TextView label = new TextView(this);
        label.setText("مبروك! \n التطبيق يعمل بنجاح 🚀");
        label.setTextSize(30);
        label.setGravity(Gravity.CENTER);
        
        // عرض النص على الشاشة
        setContentView(label);
    }
}
