package com.jetpack.stepcounter.prefrence

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.jetpack.stepcounter.geofence.MyLocation

class PreferenceHelper {
    companion object {

        private val helper = PreferenceHelper()
        private lateinit var preferences: SharedPreferences

        fun getPref(context: Context): PreferenceHelper {
            preferences =
                context.getSharedPreferences(PreferenceKeys.KEY_PREF_NAME, Context.MODE_PRIVATE)
            return helper
        }
    }

    fun getPreferences(): PreferenceHelper {
        return PreferenceHelper()
    }




    fun saveCurrentLocation(myLocation: MyLocation) {
        val gson = Gson()
        val json = gson.toJson(myLocation)
        preferences.edit().putString(PreferenceKeys.KEY_PREF_LOCATION, json).apply()

    }

    fun getCurrentLocation(): MyLocation? {
        val gson = Gson()
        val json = preferences.getString(PreferenceKeys.KEY_PREF_LOCATION, null)
        json?.let {
            return gson.fromJson(json, MyLocation::class.java)
        }
        return null
    }

    fun saveBooleanValue(key: String, value: Boolean) {
        preferences.edit().putBoolean(key, value).apply()
    }



    fun getBooleanValue(key: String): Boolean {
        return preferences.getBoolean(key, false)
    }


    fun saveStringValue(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    } fun saveIntValue(key: String, value: Int) {
        preferences.edit().putInt(key, value).apply()
    }

    fun getStringValue(key: String, defaultValue: String? = null): String? {
        return preferences.getString(key, defaultValue)
    }
    fun getIntValue(key: String, defaultValue: Int? = null): Int? {
        return preferences.getInt(key, defaultValue!!)
    }


}