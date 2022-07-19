package com.jetpack.stepcounter.geofence

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.jetpack.stepcounter.PowerConnectionReceiver

class service : Service() {
    private lateinit var powerConnectionReceiver: PowerConnectionReceiver
    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    override fun onCreate() {
        powerConnectionReceiver= PowerConnectionReceiver()
        super.onCreate()
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }
}