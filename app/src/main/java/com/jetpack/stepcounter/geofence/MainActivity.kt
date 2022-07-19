package com.jetpack.stepcounter;

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.jetpack.stepcounter.alarm.AlarmActivity
import com.jetpack.stepcounter.alarm.AlarmReceiver
import com.jetpack.stepcounter.alarm.ResetReceiver
import com.jetpack.stepcounter.geofence.MapsActivity
import com.jetpack.stepcounter.geofence.SetLocationActivity
import com.jetpack.stepcounter.geofence.StepCallback
import com.jetpack.stepcounter.prefrence.PreferenceHelper
import com.jetpack.stepcounter.prefrence.PreferenceKeys
import com.jetpack.stepcounter.worker.NotificationWorker
import com.jetpack.stepcounter.worker.WeatherWorker
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


const val TAG = "StepCounter"


class MainActivity : AppCompatActivity(), SensorEventListener, StepCallback {

    lateinit var callback: StepCallback

    // we have assigned sensorManger to nullable
    private var sensorManager: SensorManager? = null
    private var resetTime = false

    // Creating a variable which will give the running status
    // and initially given the boolean value as false
    private var running = false

    // Creating a variable which will counts total steps
    // a
    // nd it has been given the value of 0 float
    private var totalSteps = 0f

    // Creating a variable  which counts previous total
    // steps and it has also been given the value of 0 float
    private var previousTotalSteps = 0f
    lateinit var tv_stepsTaken: TextView


    lateinit var button: Button
    var hours: Int = 2
    private val fitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE)
        .addDataType(DataType.TYPE_STEP_COUNT_DELTA)
        .build()

    private val runningQOrLater =
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ignoreBatteryOptimization()
        tv_stepsTaken = findViewById<TextView>(R.id.title_text_view)
        FirebaseApp.initializeApp(this@MainActivity)
        callback = this
        val powerConnectionReceive = PowerConnectionReceiver()
        var filter = IntentFilter()
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED)
        filter.addAction(Intent.ACTION_POWER_CONNECTED)
        filter.addAction(Intent.ACTION_BOOT_COMPLETED)
        registerReceiver(powerConnectionReceive, filter)

        val alarmReceiver = AlarmReceiver()
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_BATTERY_LOW)
        registerReceiver(alarmReceiver, intentFilter)


        val currentTime: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        if (currentTime > "08:00" && currentTime < "20:00") {
            Toast.makeText(applicationContext, "Notification Time", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(applicationContext, "No Time", Toast.LENGTH_SHORT).show()
        }
        if (ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.ACTIVITY_RECOGNITION
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                100
            )
        } else {

            // Adding a context of SENSOR_SERVICE aas Sensor Manager
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        }
        schedAlarm(this@MainActivity)
        loadData()
        //resetSteps()


        // Adding a context of SENSOR_SERVICE aas Sensor Manager

        var hour = PreferenceHelper.getPref(applicationContext)
            .getStringValue(PreferenceKeys.KEY_PREF_HOURS, "")

        if (hour!!.isEmpty()) {
            hours = 2
        } else {
            hours = hour.toInt()
        }
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .setRequiresCharging(false)
            .build()

        val periodicWorkRequest = PeriodicWorkRequest.Builder(
            WeatherWorker::class.java, // Your worker class
            /* hours.toLong(),*/
            15,// repeating interval
            TimeUnit.MINUTES
        )
            /*.setConstraints(constraints)*/
            .build()


        val workManager = WorkManager.getInstance(this)
        workManager.enqueueUniquePeriodicWork(
            "periodicWorkRequest",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )

        workManager.getWorkInfoByIdLiveData(periodicWorkRequest.id).observeForever {
            if (it != null) {


            }
        }

        val periodicWorkRequestNotification = PeriodicWorkRequest.Builder(
            NotificationWorker::class.java, // Your worker class
            24, // repeating interval
            TimeUnit.HOURS
        ).setConstraints(constraints)
            .build()
        val workManagerNotification = WorkManager.getInstance(this)
        workManagerNotification.enqueueUniquePeriodicWork(
            "periodicNotificationRequest",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequestNotification
        )

        workManagerNotification.getWorkInfoByIdLiveData(periodicWorkRequest.id).observeForever {
            if (it != null) {


            }
        }
//
//        val workManager = WorkManager.getInstance(this)
//        val uploadWorkRequest = OneTimeWorkRequest.Builder(NotificationWorker::class.java).build()
//        workManager.enqueue(uploadWorkRequest)

        button = findViewById(R.id.btnGEo)

        button.setOnClickListener {
            val latitude =
                PreferenceHelper.getPref(applicationContext).getCurrentLocation()?.latitude
            if (latitude == null) {
                startActivity(Intent(this@MainActivity, SetLocationActivity::class.java))
            } else {
                startActivity(Intent(this@MainActivity, MapsActivity::class.java))
            }
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d(TAG, token)

            } else {
                Toast.makeText(this, "Failed to generate Token", Toast.LENGTH_LONG).show()
            }
        }
    }


    /** Initializes a custom log class that outputs both to in-app targets and logcat.  */

    companion object {
        const val MESSAGE_STATUS = "message_status"
    }

    override fun onResume() {
        super.onResume()
        running = true
        val stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        // Rate suitable for the user interface
        if (stepSensor != null) {
            sensorManager?.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
            val currentSteps = totalSteps.toInt() - previousTotalSteps.toInt()
            PreferenceHelper.getPref(applicationContext)
                .saveIntValue("Steps", currentSteps.toInt())
            // It will show the current steps to the user
            tv_stepsTaken.text = ("$currentSteps")
        }
        resetTime = PreferenceHelper.getPref(applicationContext).getBooleanValue("Reset")
        if (resetTime) {
            resetSteps()
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {

        // Calling the TextView that we made in activity_main.xml
        // by the id given to that TextView
        val is_charging =
            PreferenceHelper.getPref(applicationContext).getBooleanValue("IS_CHARGING")
        if (is_charging) {
            val savedSteps = PreferenceHelper.getPref(applicationContext).getIntValue("Steps", 0)
            tv_stepsTaken.text = ("$savedSteps")
            Toast.makeText(this@MainActivity, "Charging", Toast.LENGTH_LONG).show()
            sensorManager?.unregisterListener(this)
        } else {
            if (running) {
                // Current steps are calculated by taking the difference of total steps
                totalSteps = event!!.values[0]
                // Current steps are calculated by taking the difference of total steps
                // and previous steps
                val currentSteps = totalSteps.toInt() - previousTotalSteps.toInt()
                PreferenceHelper.getPref(applicationContext)
                    .saveIntValue("Steps", currentSteps.toInt())
                // It will show the current steps to the user
                tv_stepsTaken.text = ("$currentSteps")
                callback.subscribeSteps(currentSteps)
            }
        }
    }

    fun resetSteps() {
        tv_stepsTaken = findViewById<TextView>(R.id.title_text_view)
        previousTotalSteps = totalSteps
        // When the user will click long tap on the screen,
        // the steps will be reset to 0
        tv_stepsTaken.text = 0.toString()

        // This will save the data
        saveData()

    }


    private fun saveData() {

        // Shared Preferences will allow us to save
        // and retrieve data in the form of key,value pair.
        // In this function we will save data
        PreferenceHelper.getPref(applicationContext)
            .saveIntValue("key1", previousTotalSteps.toInt())
    }


    private fun loadData() {

        // In this function we will retrieve data
        val savedNumber: Int? = PreferenceHelper.getPref(applicationContext).getIntValue("key1", 0)
        // Log.d is used for debugging purposes
        Log.d("MainActivity", "$savedNumber")

        previousTotalSteps = savedNumber?.toFloat()!!
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // We do not have to write anything in this function for this app
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.setting) {
            startActivity(Intent(this, AlarmActivity::class.java))
        }
        if (item.itemId == R.id.action_read_data) {
            resetSteps()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 100) {

            // Checking whether user granted the permission or not.
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
                // Showing the toast message
                Toast.makeText(this@MainActivity, " Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this@MainActivity, " Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private fun schedAlarm(context: Context) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 1)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.DAY_OF_MONTH, 1)
        val date = Date(cal.timeInMillis)
        android.util.Log.e(TAG, "schedAlarm: " + date)
        val intentsOpen = Intent(this, ResetReceiver::class.java)
        intentsOpen.action = "com.jetpack.stepcounter.alarm.ACTION.RESET"
        val pendingIntent = PendingIntent.getBroadcast(context, 100, intentsOpen, 0)

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        android.util.Log.e(TAG, "schedAlarm: " + cal.timeInMillis.toString())
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            86400000,
            pendingIntent
        )
    }


    override fun onStart() {
        resetTime = PreferenceHelper.getPref(applicationContext).getBooleanValue("Reset")
        if (resetTime) {
            resetSteps()
        }
        super.onStart()
    }

    override fun subscribeSteps(steps: Int) {
        tv_stepsTaken.text = steps.toString()
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
}