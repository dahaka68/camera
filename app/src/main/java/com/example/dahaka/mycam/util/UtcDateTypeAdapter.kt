package com.example.dahaka.mycam.util

import android.annotation.SuppressLint
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.internal.bind.DateTypeAdapter
import com.google.gson.internal.bind.util.ISO8601Utils
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.IOException
import java.text.DateFormat
import java.text.ParseException
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for Date. Although this class appears stateless, it is not.
 * DateFormat captures its time zone and locale when it is created, which gives
 * this class state. DateFormat isn't thread safe either, so this class has
 * to synchronize its read and write methods.
 */
class UtcDateTypeAdapter : TypeAdapter<Date>() {

    @SuppressLint("SimpleDateFormat")
    private val mUtcFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private val mLocalFormat = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT)

    init {
        mUtcFormat.timeZone = TimeZone.getTimeZone("UTC")
    }

    @Throws(IOException::class)
    override fun read(`in`: JsonReader): Date? {
        if (`in`.peek() == JsonToken.NULL) {
            `in`.nextNull()
            return null
        }
        return deserializeToDate(`in`.nextString())
    }

    @Synchronized
    private fun deserializeToDate(json: String): Date {
        try {
            return mLocalFormat.parse(json)
        } catch (ignored: ParseException) {
            ignored.printStackTrace()
        }

        try {
            return mUtcFormat.parse(json)
        } catch (ignored: ParseException) {
            ignored.printStackTrace()
        }

        try {
            return ISO8601Utils.parse(json, ParsePosition(0))
        } catch (e: ParseException) {
            throw JsonSyntaxException(json, e)
        }

    }

    @Synchronized
    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: Date?) {
        if (value == null) {
            out.nullValue()
            return
        }
        val dateFormatAsString = mUtcFormat.format(value)
        out.value(dateFormatAsString)
    }

    companion object {
        val FACTORY: TypeAdapterFactory = object : TypeAdapterFactory {
            override// we use a runtime check to make sure the 'T's equal
            fun <T> create(gson: Gson, typeToken: TypeToken<T>): TypeAdapter<T>? {
                return if (typeToken.rawType == Date::class.java)
                    DateTypeAdapter() as TypeAdapter<T>
                else
                    null
            }
        }
    }
}