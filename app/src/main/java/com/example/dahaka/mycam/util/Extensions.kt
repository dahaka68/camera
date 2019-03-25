package com.example.dahaka.mycam.util

import android.content.Context
import android.support.annotation.StringRes
import android.support.v4.app.Fragment
import android.widget.Toast

fun Context?.toast(@StringRes textId: Int, duration: Int = Toast.LENGTH_LONG) =
        this?.let { Toast.makeText(it, textId, duration).show() }

fun Context?.toast(text: String, duration: Int = Toast.LENGTH_LONG) =
        this?.let { Toast.makeText(it, text, duration).show() }

fun Fragment?.toast(@StringRes textId: Int, duration: Int =
        Toast.LENGTH_LONG) = this?.let { activity.toast(textId, duration) }
