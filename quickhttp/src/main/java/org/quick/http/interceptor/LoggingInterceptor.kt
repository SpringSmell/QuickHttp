package org.quick.http.interceptor

import android.util.Log
import okhttp3.*
import org.quick.http.Utils

/**
 * 全局拦截器
 */
class LoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.currentTimeMillis()

        val response = chain.proceed(request)

        Log.d("HttpService"," ")
        Log.d("HttpService","----Request-----")
        Log.d("HttpService","----url        = " + request.url.toString())

        val resultStr = try {
            String(response.body!!.bytes())
        } catch (O_O: OutOfMemoryError) {
            "内存溢出"
        }

        if (request.method == "POST")
            Log.d("HttpService",String.format("----params     = %s", parseRequest(request)))

        Log.d("HttpService",String.format("----result     = %s", resultStr))
        Log.d("HttpService",String.format("----Response---- %d ms", System.currentTimeMillis() - startTime))
        Log.d("HttpService"," ")

        return response.newBuilder()
                .body(ResponseBody.create(Utils.mediaTypeJson, resultStr))
                .build()
    }

    companion object {
        fun parseRequest(request: Request): String {
            var params = ""
            val requestBody = request.body
            when (requestBody) {
                is FormBody -> {
                    params += parseFormBody(requestBody)
                }
                is MultipartBody -> {
                    requestBody.parts.forEach {
                        val body = it.body
                        params += when (body) {
                            is FormBody -> parseFormBody(body)
                            else -> String.format("{ %s } ", it.headers?.value(0))
                        }
                    }
                }
            }
            return params
        }

        fun parseFormBody(formBody: FormBody): String {
            var params = ""
            for (index in 0 until formBody.size)
                params += formBody.encodedName(index) + " = " + formBody.encodedValue(index) + " , "

            return String.format("{ %s }", if (params.length > 2) params.substring(0, params.length - 2) else params)
        }
    }
}