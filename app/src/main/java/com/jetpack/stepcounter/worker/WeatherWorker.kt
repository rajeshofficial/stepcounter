package com.jetpack.stepcounter.worker

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.location.Location
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.core.utilities.Utilities
import com.jetpack.stepcounter.R
import com.jetpack.stepcounter.prefrence.PreferenceHelper
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*


public class WeatherWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    private var weather_url1 = ""

    // api id for url
    private var api_id1 = "030314b750cc43e7b39e503dfe37150c"
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun doWork(): Result {
        Log.d("oneTimeWorkRequest", "Weather worker doing job")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
        Log.e("lat", weather_url1)
        val currentTime: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        if (currentTime > "08:00" && currentTime < "20:00") {
            obtainLocation()
        } else {
            Toast.makeText(applicationContext, "No Time", Toast.LENGTH_SHORT).show()
        }



        return Result.success()
    }

    @SuppressLint("MissingPermission")
    private fun obtainLocation() {
        Log.e("lat", "function")
        // get the last location
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                // get the latitude and longitude
                // and create the http URL
                weather_url1 =
                    "https://api.weatherbit.io/v2.0/current?" + "lat=" + location?.latitude + "&lon=" + location?.longitude + "&key=" + api_id1
                Log.e("lat", weather_url1.toString())
                // this function will
                // fetch data from URL
                getTemp()
            }
    }

    private fun sendNotification(title: String, message: String) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        //If on Oreo then notification required a notification channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel("default", "Default", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
        val notification: NotificationCompat.Builder = NotificationCompat.Builder(
            applicationContext,
            "default"
        )
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.mipmap.ic_launcher)
        notificationManager.notify(1, notification.build())
    }

    private fun getTemp() {
        // Instantiate the RequestQueue.
        val queue = newRequestQueue(applicationContext)
        val url: String = weather_url1
        Log.e("lat", url)

        // Request a string response
        // from the provided URL.
        val stringReq = StringRequest(
            Request.Method.GET, url,
            { response ->
                Log.e("lat", response.toString())

                // get the JSON object
                val obj = JSONObject(response)

                // get the Array from obj of name - "data"
                val arr = obj.getJSONArray("data")
                Log.e("lat obj1", arr.toString())

                // get the JSON object from the
                // array at index position 0
                val obj2 = arr.getJSONObject(0)
                Log.e("lat obj2", obj2.toString())

                // set the temperature and the city
                // name using getString() function
                var temp =
                    obj2.getString("temp") + " deg Celsius in " + obj2.getString("city_name") + "\n" + " wind speed is " + obj2.getString(
                        "wind_spd"
                    ) + "\n" + " Humidity is " + obj2.getString("rh")
                PreferenceHelper.getPref(applicationContext).saveStringValue("CacheWeather",temp)

                sendNotification("Weather Update", temp)
            },
            // In case of any error
            {
            val temp=    PreferenceHelper.getPref(applicationContext).getStringValue("CacheWeather")

                if (temp != null) {
                    sendNotification("Weather", temp
                    )  }
            })
        queue.add(stringReq)
    }

    companion object {
        private const val WORK_RESULT = "work_result"
    }
}