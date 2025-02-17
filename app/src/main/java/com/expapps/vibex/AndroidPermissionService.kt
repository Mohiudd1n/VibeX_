package com.expapps.vibex

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker

internal class AndroidPermissionService {
    /**
     * @see PermissionChecker.checkSelfPermission
     */
    fun checkSelfPermission(context: Context, permission: String): Int {
        return PermissionChecker.checkSelfPermission(context, permission)
    }

    /**
     * @see ActivityCompat.requestPermissions
     */
    fun requestPermissions(
        activity: ActivityResultLauncher<Array<String?>>? = null, permissions: Array<String?>,
        requestCode: Int
    ) {
        if (activity == null) {
            return
        }
        activity.launch(permissions)
    }

    /**
     * @see ActivityCompat.shouldShowRequestPermissionRationale
     */
    fun shouldShowRequestPermissionRationale(activity: Activity?,
                                             permission: String
    ): Boolean {
        return if (activity == null) {
            false
        } else ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    fun isPermissionPermanentlyDenied(
        activity: Activity?,
        permission: String
    ): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            false
        } else !shouldShowRequestPermissionRationale(activity, permission)
    }
}