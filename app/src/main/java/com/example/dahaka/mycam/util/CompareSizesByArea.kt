package com.example.dahaka.mycam.util

import android.os.Build
import android.support.annotation.RequiresApi
import android.util.Size
import java.lang.Long.signum
import java.util.Comparator

internal class CompareSizesByArea : Comparator<Size> {
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun compare(lhs: Size, rhs: Size) =
            signum(lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height)
}