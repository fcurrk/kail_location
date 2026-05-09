package com.mini.location.xposed.hooks.fused

import android.location.Location
import com.mini.location.xposed.base.BaseLocationHook
import com.mini.location.xposed.utils.BlindHookLocation
import com.mini.location.xposed.utils.FakeLoc
import com.mini.location.utils.MiniLog
import com.mini.location.xposed.utils.hookMethodAfter
import com.mini.location.xposed.utils.toClass

object AndroidFusedLocationProviderHook: BaseLocationHook() {
    operator fun invoke(classLoader: ClassLoader) {
        val cFusedLocationProvider = "com.android.location.fused.FusedLocationProvider".toClass(classLoader)
        if (cFusedLocationProvider == null) {
            MiniLog.w(null, "Mini_Xposed", "Failed to find FusedLocationProvider")
            return
        }

        if(!initDivineService("AndroidFusedLocationProvider")) {
            MiniLog.e(null, "Mini_Xposed", "Failed to init DivineService in AndroidFusedLocationProvider")
            return
        }

        cFusedLocationProvider.hookMethodAfter("chooseBestLocation", Location::class.java, Location::class.java) {
            if (result == null) return@hookMethodAfter

            if (FakeLoc.enable) {
                result = injectLocation(result as Location)
            }
        }

//        cFusedLocationProvider.hookMethodBefore("reportBestLocationLocked") {
//
//        }

        val cChildLocationListener = "com.android.location.fused.FusedLocationProvider\$ChildLocationListener".toClass(classLoader)
        if (cChildLocationListener == null) {
            MiniLog.w(null, "Mini_Xposed", "Failed to find ChildLocationListener")
            return
        }

        BlindHookLocation(cChildLocationListener, classLoader)
    }
}