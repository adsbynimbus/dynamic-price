package com.adsbynimbus.dynamicprice.sample

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.startup.Initializer
import com.adsbynimbus.Nimbus
import kotlin.time.measureTime

class AppInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        val nimbusStartup = measureTime {
            Nimbus.initialize(context, BuildConfig.PUBLISHER_KEY, BuildConfig.API_KEY)
            Nimbus.testMode = true
        }

        Log.i("DynamicPrice", "Nimbus init time: $nimbusStartup")
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> = mutableListOf()
}


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
}
