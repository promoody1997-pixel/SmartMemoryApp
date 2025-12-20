package com.smartmemory.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.net.Uri
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class MainActivity : AppCompatActivity() {

    lateinit var etNote: EditText
    private val SPEECH_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etNote = findViewById(R.id.etNote)
        val btnRecord = findViewById<Button>(R.id.btnRecord)
        
        btnRecord.setOnClickListener {
            askSpeechInput()
        }

        findViewById<Button>(R.id.btnPerm).setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivity(intent)
            }
        }

        setupAlarmButton(R.id.btn15min, 15)
        setupAlarmButton(R.id.btn30min, 30)
        setupAlarmButton(R.id.btn1hour, 60)
    }

    private fun askSpeechInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE)
        } catch (e: Exception) {
            Toast.makeText(this, "المايكروفون غير مدعوم", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            etNote.setText(result?.get(0))
        }
    }

    private fun setupAlarmButton(btnId: Int, minutes: Int) {
        findViewById<Button>(btnId).setOnClickListener {
            val note = etNote.text.toString()
            if(note.isEmpty()){
                Toast.makeText(this, "سجل ملاحظة أولاً", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            setAlarm(minutes, note)
        }
    }

    private fun setAlarm(minutes: Int, note: String) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        intent.putExtra("NOTE", note)
        
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        
        val timeInMillis = System.currentTimeMillis() + (minutes * 60 * 1000)
        
        // استخدام المنبه الدقيق
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
        Toast.makeText(this, "تم ضبط المنبه بعد $minutes دقيقة", Toast.LENGTH_LONG).show()
    }
}
