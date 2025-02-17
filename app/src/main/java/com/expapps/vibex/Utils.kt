package com.expapps.vibex

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import org.apache.commons.lang3.time.DurationFormatUtils
import java.util.Locale
import java.util.concurrent.TimeUnit

object Utils {
    fun checkEmptyOrNullString(vararg strings: String?): Boolean {
        if (strings.contains(null) || strings.contains("null") || strings.contains("")) {
            return false
        }
        return true
    }

    fun checkPasswordLength(str: String, length: Int = 6): Boolean {
        if (str.length > length) {
            return true
        }
        return false
    }

    fun formattedTime(ms: Int): String? {
        var tim = ""
        var secStrng = ""
        var minStrng = ""
        val s = (ms % 60)
        val m = (ms / 60) % 60
        val h = (ms / (60 * 60) % 24)
        if (h > 0) {
            tim = "$h:"
        }
        secStrng = if (s < 10) {
            "0$s"
        } else {
            "" + s
        }
        minStrng = if (m < 10) {
            "0$m"
        } else {
            "" + m
        }
        tim = "$tim$minStrng:$secStrng"
        return tim
    }

    fun formatTimeStamp(milliseconds: Int): String? {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds.toLong())
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds.toLong()) -
                TimeUnit.MINUTES.toSeconds(minutes)
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

    }

    fun isAllStringsEqual(vararg strings: String?): Boolean {
        var str1 = ""
        if (strings.isNotEmpty()) {
            str1 = strings[0] ?: ""
        }
        strings.forEach {
            if (str1 != it) {
                return false
            }
        }
        return true
    }

    fun getMCodeFromUserId(userId: String): String {
        if (checkEmptyOrNullString(userId)) {
            val uidLen = userId.length
            if (uidLen > 6) {
                return userId.substring(uidLen - 6, uidLen)
            }
        }
        return ""
    }

    fun getEmailFromEmailId(str: String): String {
        val emailId = str.split("@")
        if (emailId.isNotEmpty()) {
            return emailId[0]
        }
        return ""
    }

    fun Context?.showToast(message: String?, length: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, message, length).show()
    }

    fun FragmentActivity?.openActivity(clazz: Class<*>, finishPrev: Boolean = false) {
        val intent = Intent(this, clazz)
        this?.startActivity(intent)
        if (finishPrev) {
            this?.finish()
        }
    }
}