package com.smartmemory.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val note = intent.getStringExtra("NOTE") ?: "تنبيه!"
        
        val i = Intent(context, OverlayActivity::class.java)
        i.putExtra("NOTE", note)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        context.startActivity(i)
    }
}
