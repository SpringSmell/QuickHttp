package org.quick.http

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.json.JSONArray
import java.util.ArrayList

object JsonUtils {

    val moshi: Moshi by lazy { return@lazy Moshi.Builder().build() }

    inline fun <reified T> parseFromJson(json: String?): T? = try {
        val jsonAdapter = moshi.adapter<T>(T::class.java)
        jsonAdapter.fromJson(json)
    } catch (O_O: Exception) {
        Log.e(JsonUtils::class.java.simpleName, "json or class error , from  " + T::class.java.simpleName + " \nerror json :" + json)
        O_O.printStackTrace()
        null
    }

    /**
     * 将json解析成java对象
     *
     * @param json
     * @param cls
     * @return
     */
    fun <T> parseFromJson(json: String, cls: Class<T>, vararg clss: Class<T>): T? = try {
        val isList = when (cls.canonicalName) {
            "java.util.ArrayList" -> true
            "java.util.List" -> true
            else -> false
        }
        val type = if (isList)
            Types.newParameterizedType(List::class.java, *clss)
        else
            Types.newParameterizedType(cls, *clss)
        moshi.adapter<T>(type).fromJson(json)
    } catch (ex: Exception) {
        ex.printStackTrace()
        Log.e(JsonUtils::class.java.simpleName, "json or class error , from  " + cls.simpleName + " \nerror json :" + json)
        null
    }

    /**
     * 将json解析成java对象
     *
     * @param json
     * @param cls
     * @return
     */
    fun <T> parseFromJson(json: String?, cls: Class<T>): T? = try {
        val jsonAdapter = moshi.adapter<T>(cls)
        jsonAdapter.fromJson(json)
    } catch (ex: Exception) {
        ex.printStackTrace()
        Log.e(JsonUtils::class.java.simpleName, "json or class error , from  " + cls.simpleName + " \nerror json :" + json)
        null
    }

    /**
     * 将json解析为java对象列表
     *
     * @param json
     * @param cls
     * @return
     */
    fun <T> parseFromJsons(json: String, cls: Class<T>): List<T> {
        val listT = ArrayList<T>()
        try {
            val ja = JSONArray(json)
            (0 until ja.length()).mapTo(listT) { parseFromJson(ja.getString(it), cls)!! }
        } catch (ex: Exception) {
            ex.printStackTrace()
            Log.e(JsonUtils::class.java.simpleName, "json or class error , from  " + cls.simpleName + " \nerror json :" + json)
        }

        return listT
    }

    /**
     * 将json解析为java对象列表
     *
     * @param json
     * @param cls
     * @return
     */
    inline fun <reified T> parseFromJsons(json: String?): List<T> {
        val listT = ArrayList<T>()
        try {
            val ja = JSONArray(json)
            (0 until ja.length()).mapTo(listT) { parseFromJson(ja.getString(it), T::class.java)!! }
        } catch (ex: Exception) {
            ex.printStackTrace()
            Log.e(JsonUtils::class.java.simpleName, "json or class error , from  " + T::class.java.simpleName + " \nerror json :" + json)
        }

        return listT
    }

    /**
     * 将对象解析为json
     *
     * @param obj
     * @return
     */
    fun parseToJson(obj: Any): String = try {
        val jsonAdapter = moshi.adapter(Any::class.java)
        jsonAdapter.toJson(obj)
    } catch (ex: Exception) {
        ex.printStackTrace()
        Log.e(JsonUtils::class.java.simpleName, "class error , from " + obj::class.java.simpleName)
        ""
    }
}