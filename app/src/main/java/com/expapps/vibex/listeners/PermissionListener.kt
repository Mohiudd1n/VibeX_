package com.expapps.vibex.listeners

interface PermissionListener {
    fun onNext(isGranted: Boolean)
    fun onMultipleNext(permissionName: String, isGranted: Boolean)
}