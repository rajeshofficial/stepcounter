package com.jetpack.stepcounter.alarm

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.jetpack.stepcounter.MainActivity
import com.jetpack.stepcounter.R
import com.jetpack.stepcounter.prefrence.PreferenceHelper
import com.jetpack.stepcounter.prefrence.PreferenceKeys
import java.util.*
import java.util.concurrent.TimeUnit


class AlarmActivity : AppCompatActivity(), View.OnClickListener {

    var btnStartAlarm: Button? = null
    var btnStopAlarm: Button? = null
    var context: Context? = null
    var time: EditText? = null
    var savedtime: String? =null
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm)

        context = this@AlarmActivity

        val intentsOpen = Intent(this, AlarmReceiver::class.java)
        intentsOpen.action = "com.jetpack.stepcounter.alarm.ACTION"
        pendingIntent = PendingIntent.getBroadcast(this, 111, intentsOpen, 0)
        alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        btnStartAlarm = findViewById<View>(R.id.button1) as Button
        btnStopAlarm = findViewById<View>(R.id.button2) as Button
        time = findViewById<EditText>(R.id.etTime) as EditText

        btnStartAlarm!!.setOnClickListener(this)
        btnStopAlarm!!.setOnClickListener(this)

        savedtime= PreferenceHelper.getPref(applicationContext).getStringValue("Time")
        if(savedtime !=null) {
            time!!.setText(savedtime)
            val batteryLow =
                PreferenceHelper.getPref(applicationContext).getBooleanValue("BATTERY_LOW")
            val hoursInMillis=TimeUnit.HOURS.toMillis(savedtime!!.toLong())
            if (batteryLow) {
                fireAlarm(hoursInMillis *2)
            } else {
                fireAlarm(hoursInMillis)
            }
        } else{
            val hoursInMillis = TimeUnit.HOURS.toMillis(2)
            fireAlarm(hoursInMillis)
        }
    }
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onClick(v: View) {
        // TODO Auto-generated method stub
        if (v === btnStartAlarm) {
            savedtime= PreferenceHelper.getPref(applicationContext).getStringValue("Time")
            if(savedtime !=null){
                time!!.setText(savedtime)
                val time=time!!.text.toString()
                val hoursInMillis=TimeUnit.HOURS.toMillis(time.toLong())
                fireAlarm(hoursInMillis)

            }else if(time!!.text.toString().equals("")) {
                val hoursInMillis = TimeUnit.HOURS.toMillis(2)
                fireAlarm(hoursInMillis)
            }else{
               val timeUnit= time!!.text.toString()
                PreferenceHelper.getPref(applicationContext).saveStringValue("Time",timeUnit)
                val hoursInMillis=TimeUnit.HOURS.toMillis(timeUnit.toLong())
                fireAlarm(hoursInMillis)
            }

            startActivity(Intent(this,MainActivity::class.java))

            }
        if (v === btnStopAlarm) {
            stopAlarm()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun fireAlarm(hoursInMillis: Long) {
        ignoreBatteryOptimization()
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = System.currentTimeMillis()
            alarmManager!!.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                hoursInMillis,
                pendingIntent
            )

    }
    fun stopAlarm() {
        alarmManager!!.cancel(pendingIntent)
    }

    companion object {
        var pendingIntent: PendingIntent? = null
        var alarmManager: AlarmManager? = null
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun ignoreBatteryOptimization() {
        val intent = Intent()
        val packN = packageName
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packN)) {
            intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            intent.data = Uri.parse("package:$packN")
            startActivity(intent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onResume() {
        super.onResume()
        }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onStart() {
        super.onStart()
    }

}
