package com.jetpack.stepcounter.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.jetpack.stepcounter.prefrence.PreferenceHelper

class ResetReceiver : BroadcastReceiver() {
    private val SOMEACTION = "com.jetpack.stepcounter.alarm.ACTION.RESET"

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
        if (action == SOMEACTION) {
            Log.e("TAG", "onReceive: ")
            if (context != null) {
                PreferenceHelper.getPref(context.applicationContext).saveBooleanValue("Reset", true)
            }
        } else {
            context?.applicationContext?.let {
                PreferenceHelper.getPref(it).saveBooleanValue("Reset", false)
            }
        }
    }
    }