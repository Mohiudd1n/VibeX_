package com.expapps.vibex.auth

data class User(
    var userId: String? = null,
    var email: String? = null,
    var password: String? = null,
    var mcode: String? = null,
    var userRegistrationCompleted: Boolean = false
)