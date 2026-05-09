package com.mini.location.xposed.base

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import com.mini.location.xposed.utils.BinderUtils
import com.mini.location.xposed.utils.FakeLoc
import com.mini.location.utils.MiniLog

abstract class BaseDivineService {
    /**
     * if the hook is TelephonyService? or other service?
     * this service may not be in the same space as the `system_server`,
     * so a binder is used to talk.
     */
    protected fun initDivineService(from: String, retryCount: Int = 0): Boolean {
        // mini_location does not use the mini provider divine service model,
        // so we always return true to allow hooks to proceed.
        return true
    }

    /**
     * Synchronize configurations in different processes
     */
    private fun syncConfig(locationManager: LocationManager, randomKey: String) {
        // No-op in mini_location
    }
}
