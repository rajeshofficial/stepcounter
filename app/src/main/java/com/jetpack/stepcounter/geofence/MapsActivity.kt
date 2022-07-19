package com.jetpack.stepcounter.geofence

import android.Manifest
import android.app.*
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.maps.OnMapReadyCallback
import com.h6ah4i.android.widget.verticalseekbar.VerticalSeekBar
import com.google.android.gms.maps.GoogleMap
import android.content.ServiceConnection
import android.content.ComponentName
import android.os.IBinder
import com.jetpack.stepcounter.geofence.MapsActivity.GeoService.RunServiceBinder
import com.google.android.gms.maps.SupportMapFragment
import com.jetpack.stepcounter.R
import android.os.Bundle
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.SeekBar
import com.google.android.gms.maps.CameraUpdateFactory
import android.content.pm.PackageManager
import com.jetpack.stepcounter.geofence.MapsActivity.GeoService.LocationChangeListener
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.LatLng
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.ConnectionResult
import android.widget.Toast
import com.google.android.gms.maps.model.CircleOptions
import android.content.Intent
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.database.DatabaseReference
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoQuery
import com.google.firebase.database.FirebaseDatabase
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQueryEventListener
import androidx.annotation.RequiresApi
import android.os.Build
import com.google.firebase.database.DatabaseError
import androidx.core.app.NotificationCompat
import android.content.Context
import android.graphics.Color
import android.location.Location
import android.os.Binder
import android.util.Log
import android.view.View
import android.widget.Button
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.Marker
import com.jetpack.stepcounter.prefrence.PreferenceHelper

class MapsActivity : FragmentActivity(), OnMapReadyCallback {
    var mCurrent: Marker? = null
    var mVerticalSeekBar: VerticalSeekBar? = null
    private var mMap: GoogleMap? = null
    private val mLastLocaiton: Location? = null
    private var geoService: GeoService? = null
    private var serviceBound = false
    private  var button: Button?=null

    /**
     * Callback for service binding, passed to bindService()
     */
    private val mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "Service bound")
            }
            val binder = service as RunServiceBinder
            geoService = binder.service
            serviceBound = true
            // Ensure the service is not in the foreground when bound
            geoService!!.background()
            setUpdateLocation()
            val mapFragment = supportFragmentManager
                .findFragmentById(R.id.maps) as SupportMapFragment
            mapFragment.getMapAsync(this@MapsActivity)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "Service disconnect")
            }
            serviceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        geoService?.foreground()
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mVerticalSeekBar = findViewById<View>(R.id.verticalSeekBar) as VerticalSeekBar
        button= findViewById<View>(R.id.btnChange) as Button
        button?.setOnClickListener{
            startActivity(Intent(this,SetLocationActivity::class.java))
        }
        mVerticalSeekBar!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                mMap!!.animateCamera(CameraUpdateFactory.zoomTo(progress.toFloat()), 1500, null)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}



        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MY_PERMISSION_REQUEST_CODE) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (checkPlayService()) {
                    geoService!!.buildGoogleApiClient()
                    geoService!!.createLocationRequest()
                    geoService!!.displayLocation()
                    geoService!!.setLocationChangeListener(object : LocationChangeListener {
                        override fun onLocationChange(location: Location) {
                            if (mCurrent != null) mCurrent!!.remove()
                            mCurrent = mMap!!.addMarker(
                                MarkerOptions()
                                    .position(LatLng(location.latitude, location.longitude))
                                    .title("You")
                            )
                            val coordinate = LatLng(location.latitude, location.longitude)
                            val yourLocation = CameraUpdateFactory.newLatLngZoom(coordinate, 12f)
                            mMap!!.animateCamera(yourLocation)
                        }
                    })
                }
            }
        }
    }

    private fun setUpdateLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ), MY_PERMISSION_REQUEST_CODE
            )
        } else {
            if (checkPlayService()) {
                geoService!!.buildGoogleApiClient()
                geoService!!.createLocationRequest()
                geoService!!.displayLocation()
                geoService!!.setLocationChangeListener(object : LocationChangeListener {
                    override fun onLocationChange(location: Location) {
                        if (mCurrent != null) mCurrent!!.remove()
                        mCurrent = mMap!!.addMarker(
                            MarkerOptions()
                                .position(LatLng(location.latitude, location.longitude))
                                .title("You")
                        )
                        val coordinate = LatLng(location.latitude, location.longitude)
                        val yourLocation = CameraUpdateFactory.newLatLngZoom(coordinate, 12f)
                        mMap!!.animateCamera(yourLocation)
                    }
                })
            }
        }
    }

    private fun checkPlayService(): Boolean {
        val googleAPI = GoogleApiAvailability.getInstance()
        val result = googleAPI.isGooglePlayServicesAvailable(this)
        if (result != ConnectionResult.SUCCESS) {
            if (googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog(this, result, PLAY_SERVICE_RESULATION_REQUEST)!!
                    .show()
            } else {
                Toast.makeText(this, "This Device is not supported.", Toast.LENGTH_SHORT).show()
            }
            return false
        }
        return true
    }

    override fun onMapReady(googleMap: GoogleMap) {
        val myLocation=PreferenceHelper.getPref(applicationContext).getCurrentLocation()
        val lat=myLocation?.latitude
        val long=myLocation?.longitude
        mMap = googleMap
        val dangerous_area = LatLng(lat!!, long!!)
        mMap!!.addCircle(
            CircleOptions()
                .center(dangerous_area)
                .radius(10.0)
                .strokeColor(Color.BLUE)
                .fillColor(0x220000FF)
                .strokeWidth(5.0f)
        )
        geoService!!.startService(dangerous_area, .01)
    }

    override fun onStart() {
        super.onStart()
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "Starting and binding service")
        }
        val i = Intent(this, GeoService::class.java)
        startService(i)
        bindService(i, mConnection, 0)
    }

    override fun onStop() {
        super.onStop()
        if (serviceBound) {
            // If a timer is active, foreground the service, otherwise kill the service
            if (geoService!!.isServiceRunning) {
                geoService!!.foreground()
            } else {
                geoService!!.foreground()
            }
            // Unbind the service
          // unbindService(mConnection)
          //  serviceBound = false
        }
    }

    class GeoService : Service(), GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {
        // Service binder
        private val serviceBinder: IBinder = RunServiceBinder()
        private var mLocationRequest: LocationRequest? = null
        private var mGoogleApiClient: GoogleApiClient? = null
        private var mLastLocation: Location? = null
        private var ref: DatabaseReference? = null
        private var geoFire: GeoFire? = null
        private var mLocationChangeListener: LocationChangeListener? = null

        /**
         * @return whether the service is running
         */
        // Is the service tracking time?
        var isServiceRunning = false
            private set
        private var geoQuery: GeoQuery? = null
        override fun onCreate() {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "Creating service")
            }
            ref = FirebaseDatabase.getInstance().getReference("MyLocation")
            geoFire = GeoFire(ref)
            isServiceRunning = false
        }

        override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "Starting service")
            }
            return START_STICKY
        }

        override fun onBind(intent: Intent): IBinder? {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "Binding service")
            }
            return serviceBinder
        }

        override fun onDestroy() {
            super.onDestroy()
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "Destroying service")
            }
        }

        /**
         * Starts the timer
         */
        fun startService(latLng: LatLng, radius: Double) {
            if (!isServiceRunning) {
                isServiceRunning = true
            } else {
                Log.e(TAG, "startService request for an already running Service")
            }
            if (geoQuery != null) {
                geoQuery!!.removeAllListeners()
           }
            geoQuery = geoFire!!.queryAtLocation(GeoLocation(latLng.latitude, latLng.longitude), radius)
            geoQuery!!.addGeoQueryEventListener(object : GeoQueryEventListener {
                override fun onKeyEntered(key: String, location: GeoLocation) {
                    showNotification("MRF", String.format("%s entered the dangerous area", key))
                    Toast.makeText(applicationContext,"Entered",Toast.LENGTH_LONG).show()

                }

                override fun onKeyExited(key: String) {
                    showNotification("MRF", String.format("%s exit the dangerous area", key))
                    Toast.makeText(applicationContext,"Exited",Toast.LENGTH_LONG).show()

                }

                override fun onKeyMoved(key: String, location: GeoLocation) {
                    Log.d(
                        "MOVE",
                        String.format(
                            "%s move within the dangerous area [%f/%f]",
                            key,
                            location.latitude,
                            location.longitude
                        )
                    )
                    Toast.makeText(applicationContext,"MOVE",Toast.LENGTH_LONG).show()

                }

                override fun onGeoQueryReady() {
                    Toast.makeText(applicationContext,"READY",Toast.LENGTH_LONG).show()
                }
                override fun onGeoQueryError(error: DatabaseError) {
                    Toast.makeText(applicationContext,"ERROR",Toast.LENGTH_LONG).show()
                    Log.d("ERROR", "" + error)
                }
            })
        }

        /**
         * Stops the timer
         */
        fun stopService() {
            if (isServiceRunning) {
                isServiceRunning = false
                geoQuery!!.removeAllListeners()
            } else {
                Log.e(TAG, "stopTimer request for a timer that isn't running")
            }
        }

        /**
         * Place the service into the foreground
         */
        fun foreground() {
            startForeground(NOTIFICATION_ID, sendNotification("Service Active","Forground.."))
        }

        /**
         * Return the service to the background
         */
        fun background() {
            startForeground(NOTIFICATION_ID, sendNotification("Service Active","Background.."))

        }

        /**
         * Creates a notification for placing the service into the foreground
         onS*
         * @return a notification for interacting with the service when in the foreground
         */
        private fun createNotification(): Notification {
            val builder = NotificationCompat.Builder(this)
                .setContentTitle("Service is Active")
                .setContentText("Tap to return to the Map")
                .setSmallIcon(R.mipmap.ic_launcher)
            val resultIntent = Intent(this, MapsActivity::class.java)
            val resultPendingIntent = PendingIntent.getActivity(
                this, 0, resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.setContentIntent(resultPendingIntent)
            return builder.build()
        }

//        @RequiresApi(api = Build.VERSION_CODES.O)
//        private fun sendNotification(title: String, content: String) {
//            val builder = Notification.Builder(this)
//                .setPriority(Notification.PRIORITY_DEFAULT)
//                .setChannelId("MyChannelId")
//                .setSmallIcon(R.mipmap.ic_launcher)
//                .setContentTitle(title)
//                .setContentText(content)
//            val manager = this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
//            val intent = Intent(this, MapsActivity::class.java)
//            val contentIntent =
//                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
//            builder.setContentIntent(contentIntent)
//            val notification = builder.build()
//            notification.flags = notification.flags or Notification.FLAG_AUTO_CANCEL
//            notification.defaults = notification.defaults or Notification.DEFAULT_SOUND
//            startForeground(1, builder.build())
//        }

        private fun sendNotification(title: String, message: String): Notification? {
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

        return notification.build()}


        private fun showNotification(title: String, message: String) {
            val notificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            //If on Oreo then notification required a notification channel.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel =
                    NotificationChannel(
                        "default",
                        "Default",
                        NotificationManager.IMPORTANCE_DEFAULT
                    )
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



        override fun onConnected(bundle: Bundle?) {
            displayLocation()
            startLocationUpdate()
        }

        private fun startLocationUpdate() {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient!!,
                mLocationRequest!!,
                this
            )
        }

        override fun onConnectionSuspended(i: Int) {
            mGoogleApiClient!!.connect()
        }

        override fun onConnectionFailed(connectionResult: ConnectionResult) {}
        override fun onLocationChanged(location: Location) {
            mLastLocation = location
            displayLocation()
        }

        fun createLocationRequest() {
            mLocationRequest = LocationRequest()
            mLocationRequest!!.interval = UPDATE_INTERVAL.toLong()
            mLocationRequest!!.fastestInterval = FATEST_INTERVAL.toLong()
            mLocationRequest!!.priority =
                LocationRequest.PRIORITY_HIGH_ACCURACY
            mLocationRequest!!.smallestDisplacement = DISPLACEMENT.toFloat()
        }

        fun buildGoogleApiClient() {
            mGoogleApiClient = GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build()
            mGoogleApiClient!!.connect()
        }

        fun displayLocation() {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient!!)
            if (mLastLocation != null) {
                val latitude = mLastLocation!!.latitude
                val longitude = mLastLocation!!.longitude
                geoFire!!.setLocation("You", GeoLocation(latitude, longitude)) { key, error ->
                    if (mLocationChangeListener != null) {
                        mLocationChangeListener!!.onLocationChange(mLastLocation!!)
                    }
                }
                Log.d(
                    "MRF",
                    String.format("Your last location was chaged: %f / %f", latitude, longitude)
                )
            } else {
                Log.d("MRF", "Can not get your location.")
            }
        }

        fun setLocationChangeListener(mLocationChangeListener: LocationChangeListener?) {
            this.mLocationChangeListener = mLocationChangeListener
        }

        interface LocationChangeListener {
            fun onLocationChange(location: Location)
        }

        inner class RunServiceBinder : Binder() {
            val service: GeoService
                get() = this@GeoService
        }

        companion object {
            private val TAG = GeoService::class.java.simpleName

            // Foreground notification id
            private const val NOTIFICATION_ID = 1
        }
    }

    companion object {
        //Play Service Location
        private const val MY_PERMISSION_REQUEST_CODE = 7192
        private const val PLAY_SERVICE_RESULATION_REQUEST = 300193
        private const val TAG = "mapsActivity"
        private const val UPDATE_INTERVAL = 5000
        private const val FATEST_INTERVAL = 3000
        private const val DISPLACEMENT = 10
    }
}