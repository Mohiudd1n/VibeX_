package com.expapps.vibex

import android.widget.EditText

fun EditText?.toText(): String {
    return this?.text?.toString()?.trim() ?: ""
}

fun isStringsNotNullOrEmpty(vararg s: String): Boolean {
    s.forEach {
        if (it.isBlank()) {
            return false
        }
    }
    return true
}