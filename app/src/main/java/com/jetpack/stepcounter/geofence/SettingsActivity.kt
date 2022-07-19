package com.jetpack.stepcounter.geofence

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.jetpack.stepcounter.MainActivity
import com.jetpack.stepcounter.R
import com.jetpack.stepcounter.alarm.AlarmActivity
import com.jetpack.stepcounter.prefrence.PreferenceHelper
import com.jetpack.stepcounter.prefrence.PreferenceKeys

class SettingsActivity : AppCompatActivity() {

    lateinit var saveButton: Button
    lateinit var editTextHours: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        initViews()

        saveButton.setOnClickListener {
            var hours = editTextHours.text.toString()
            if (hours.isEmpty()) {
                Toast.makeText(applicationContext, "please enter hours", Toast.LENGTH_LONG).show()
            } else {
                saveHours(hours)
            }
        }

    }

    private fun saveHours(hours: String) {
        PreferenceHelper.getPref(applicationContext).saveStringValue(
            PreferenceKeys.KEY_PREF_HOURS,
            hours
        )
        startActivity(Intent(applicationContext, AlarmActivity::class.java))
        finish()
    }

    private fun initViews() {
        editTextHours = findViewById(R.id.et_hours)
        saveButton = findViewById(R.id.save)

    }
}