package com.example.smartmemoryapp;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // هذا السطر يستدعي ملف التصميم activity_main.xml
        setContentView(R.layout.activity_main);
    }
}
