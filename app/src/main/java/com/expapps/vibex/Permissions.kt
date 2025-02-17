package com.expapps.vibex

import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import com.expapps.vibex.listeners.PermissionListener

class Permissions {
    companion object {
        var permissionListener: PermissionListener? = null
        private var fragmentActivity: FragmentActivity? = null

        @JvmStatic
        fun withContext(fragmentActivity: FragmentActivity): Companion {
            this.fragmentActivity = fragmentActivity
            return this
        }

        @JvmStatic
        fun requestPermission(permission: String?) {
            val launcher = fragmentActivity?.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                permissionListener?.onNext(isGranted = it)
            }
            launcher?.launch(permission)
        }

        @JvmStatic
        fun requestPermissions(permissions: Array<String>) {
            val launcher = fragmentActivity?.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                it.forEach { map ->
                    permissionListener?.onMultipleNext(map.key, map.value)
                }
            }
            launcher?.launch(permissions)
        }
    }
}