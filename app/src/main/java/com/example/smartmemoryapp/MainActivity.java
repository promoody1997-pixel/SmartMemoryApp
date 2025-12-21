package com.example.smartmemoryapp;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // هذا السطر يربط الكود بملف التصميم الذي وضعناه في الخطوة 1
        setContentView(R.layout.activity_main);
    }
}
