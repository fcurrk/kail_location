package com.mini.location.xposed.utils

import android.location.Location
import de.robv.android.xposed.XposedBridge
import com.mini.location.xposed.base.BaseLocationHook
import com.mini.location.xposed.utils.FakeLoc
import com.mini.location.utils.MiniLog

object BlindHookLocation: BaseLocationHook() {
    operator fun invoke(clazz: Class<*>, classLoader: ClassLoader): Int {
        return BlindHook(clazz, classLoader) { method, location: Location? ->
            if (location == null || !FakeLoc.enable) return@BlindHook location

            val newLoc = injectLocation(location)

            if (FakeLoc.enableDebugLog) {
                MiniLog.d(null, "Mini_Xposed", "${method.name} injected: $newLoc")
            }

            newLoc
        }
    }
}