package com.jetpack.stepcounter.geofence

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.jetpack.stepcounter.R
import com.jetpack.stepcounter.TAG
import com.jetpack.stepcounter.prefrence.PreferenceHelper
import com.jetpack.stepcounter.utils.GPS
import java.lang.String
import kotlin.Array
import kotlin.Boolean
import kotlin.ClassCastException
import kotlin.Int
import kotlin.IntArray


class SetLocationActivity : AppCompatActivity() {

    lateinit var editTextAddress: EditText
    lateinit var editTextLongitude: EditText
    lateinit var editTextLatitude: EditText
    lateinit var button: Button
    private val REQUEST_ID_MULTIPLE_PERMISSIONS = 100

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_location)
        initViews()
//        showLocationPrompt()

        if (checkAndRequestPermissions()) {
            val gps = GPS(this@SetLocationActivity)
            if (gps.isGPSTrackingEnabled) {
                val stringLatitude = String.valueOf(gps.latitude)
                val stringLongitude = String.valueOf(gps.longitude)
                val addressLine = gps.getAddressLine(applicationContext)
                editTextAddress.setText(addressLine)
                editTextLatitude.setText(stringLatitude)
                editTextLongitude.setText(stringLongitude)

            } else {
                // can't get location
                // GPS or Network is not enabled
                // Ask user to enable GPS/network in settings
                gps.showSettingsAlert()
            }
        } else {
          //  Toast.makeText(this, R.string.error_permission_map, Toast.LENGTH_LONG).show();
        }




        button.setOnClickListener {
            val address = editTextAddress.text.toString()
            val lat = editTextLatitude.text.toString()
            val lon = editTextLongitude.text.toString()

            if (address.isEmpty() || lat.isEmpty() || lon.isEmpty()) {
                Toast.makeText(applicationContext, "Please fill all fields", Toast.LENGTH_LONG)
                    .show()
            } else {
                var myLocation = MyLocation(address, lat.toDouble(), lon.toDouble())
                PreferenceHelper.getPref(applicationContext).saveCurrentLocation(myLocation)
                startActivity(Intent(applicationContext, MapsActivity::class.java))
            }

        }

    }

    private fun initViews() {
        editTextAddress = findViewById(R.id.et_address)
        editTextLatitude = findViewById(R.id.et_lat)
        editTextLongitude = findViewById(R.id.et_lon)
        button = findViewById(R.id.button)
    }

    private fun showLocationPrompt() {
        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val result: Task<LocationSettingsResponse> =
            LocationServices.getSettingsClient(this@SetLocationActivity)
                .checkLocationSettings(builder.build())

        result.addOnCompleteListener { task ->
            try {
                val response = task.getResult(ApiException::class.java)
                // All location settings are satisfied. The client can initialize location
                // requests here.
            } catch (exception: ApiException) {
                when (exception.statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        try {
                            // Cast to a resolvable exception.
                            val resolvable: ResolvableApiException =
                                exception as ResolvableApiException
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            resolvable.startResolutionForResult(
                                this@SetLocationActivity, LocationRequest.PRIORITY_HIGH_ACCURACY
                            )
                        } catch (e: IntentSender.SendIntentException) {
                            // Ignore the error.
                        } catch (e: ClassCastException) {
                            // Ignore, should be an impossible error.
                        }
                    }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        // Location settings are not satisfied. But could be fixed by showing the
                        // user a dialog.

                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun checkAndRequestPermissions(): Boolean {
        val permissionCoarseLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val locationPermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val listPermissionsNeeded: MutableList<kotlin.String> = ArrayList()
        if (locationPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permissionCoarseLocation != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (listPermissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                listPermissionsNeeded.toTypedArray(),
                REQUEST_ID_MULTIPLE_PERMISSIONS
            )
            return false
        }
        return true
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<kotlin.String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG, "Permission callback called-------")
        when (requestCode) {
            REQUEST_ID_MULTIPLE_PERMISSIONS -> {
                val perms: MutableMap<kotlin.String, Int> = HashMap()
                // Initialize the map with both permissions
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] = PackageManager.PERMISSION_GRANTED
                perms[Manifest.permission.ACCESS_FINE_LOCATION] = PackageManager.PERMISSION_GRANTED
                // Fill with actual results from user
                if (grantResults.size > 0) {
                    var i = 0
                    while (i < permissions.size) {
                        perms[permissions[i]] = grantResults[i]
                        i++
                    }
                    // Check for both permissions
                    if (perms[Manifest.permission.ACCESS_COARSE_LOCATION] == PackageManager.PERMISSION_GRANTED
                        && perms[Manifest.permission.ACCESS_FINE_LOCATION] == PackageManager.PERMISSION_GRANTED
                    ) {
                        Log.d(TAG, "location services permission granted")
                        // process the normal flow
                        //else any one or both the permissions are not granted
                    } else {
                        Log.d(TAG, "Some permissions are not granted ask again ")
                        //permission is denied (this is the first time, when "never ask again" is not checked) so ask again explaining the usage of permission
//                        // shouldShowRequestPermissionRationale will return true
                        //show the dialog or snackbar saying its necessary and try again otherwise proceed with setup.
                        if (ActivityCompat.shouldShowRequestPermissionRationale(
                                this,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) || ActivityCompat.shouldShowRequestPermissionRationale(
                                this,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        ) {
                            showDialogOK(
                                "Location Services Permission required for this app"
                            ) { dialog, which ->
                                when (which) {
                                    DialogInterface.BUTTON_POSITIVE -> checkAndRequestPermissions()
                                    DialogInterface.BUTTON_NEGATIVE -> {}
                                }
                            }
                        } else {
                            Toast.makeText(
                                this,
                                "Go to settings and enable permissions",
                                Toast.LENGTH_LONG
                            )
                                .show()
                            //                            //proceed with logic by disabling the related features or quit the app.
                        }
                    }
                }
            }
        }
    }

    private fun showDialogOK(message: kotlin.String, okListener: DialogInterface.OnClickListener) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton("OK", okListener)
            .setNegativeButton("Cancel", okListener)
            .create()
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onResume() {


        if (checkAndRequestPermissions()) {
            val gps = GPS(this)
            if (gps.isGPSTrackingEnabled) {
                val stringLatitude = String.valueOf(gps.latitude)
                val stringLongitude = String.valueOf(gps.longitude)
                val addressLine = gps.getAddressLine(this)
                editTextAddress.setText(addressLine)
                editTextLatitude.setText(stringLatitude)
                editTextLongitude.setText(stringLongitude)

            } else {
                // can't get location
                // GPS or Network is not enabled
                // Ask user to enable GPS/network in settings
                gps.showSettingsAlert()
            }
        } else {
        }
        super.onResume()
}
}
