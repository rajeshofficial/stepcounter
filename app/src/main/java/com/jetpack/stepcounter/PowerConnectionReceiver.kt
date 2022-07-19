package com.jetpack.stepcounter;

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.jetpack.stepcounter.prefrence.PreferenceHelper

class PowerConnectionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_POWER_CONNECTED) {
            //Handle power connected
            sendNotification("Step Counter","Step counter is Disabled", context.applicationContext as Application?
            )
            PreferenceHelper.getPref(context.applicationContext).saveBooleanValue("IS_CHARGING",true)
        } else if (intent.action.equals( Intent.ACTION_POWER_DISCONNECTED)) {
            //Handle power disconnected
            sendNotification("Step Counter","Step counter is Enabled", context?.applicationContext as Application?
            )
            if (context != null) {
                PreferenceHelper.getPref(context.applicationContext).saveBooleanValue("IS_CHARGING",false)
            }
        }else if(intent.action.equals(Intent.ACTION_BOOT_COMPLETED)){
            PreferenceHelper.getPref(context.applicationContext).saveIntValue("key1",0)
        }

/*

        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context?.registerReceiver(null, ifilter)
        }
        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL

   */


       /* if(intent.action ==Intent.ACTION_POWER_CONNECTED){
            sendNotification("Step Counter","Step counter is Disabled",
                context.applicationContext as Application?
            )
            Log.e(TAG, "onReceive: power connected" )
            Toast.makeText(context,"power connected",Toast.LENGTH_LONG).show()
            if (context != null) {
                PreferenceHelper.getPref(context.applicationContext).saveBooleanValue("IS_CHARGING",true)
            }

        }
        if(intent?.action==Intent.ACTION_POWER_DISCONNECTED){

            Log.e(TAG, "onReceive: power disconnected" )

*/

        }

       /* if (isCharging) {
            var sensorManager: SensorManager? = null

            if (context != null) {
                sendNotification(
                    "Fitness", "Step Sensor is disabled",
                    context.applicationContext as Application?
                )
                PreferenceHelper.getPref(context.applicationContext).saveBooleanValue("IS_CHARGING",true)
            }

        } else {
            context.applicationContext?.let { PreferenceHelper.getPref(it).saveBooleanValue("IS_CHARGING",false) }
            sendNotification(
                "Fitness", "Step Sensor is Enabled",
                context.applicationContext as Application?
            )
        }*/

    }





private fun sendNotification(title: String, message: String, context: Application?) {
    val notificationManager =
        context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    //If on Oreo then notification required a notification channel.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel =
            NotificationChannel("default", "Default", NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)
    }
    val notification: NotificationCompat.Builder = NotificationCompat.Builder(
        context,
        "default"
    )
        .setContentTitle(title)
        .setContentText(message)
        .setSmallIcon(R.mipmap.ic_launcher)
    notificationManager.notify(1, notification.build())
}



