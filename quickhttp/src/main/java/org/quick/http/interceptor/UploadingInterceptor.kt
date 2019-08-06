package org.quick.http.interceptor

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 上传拦截器
 */
class UploadingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.currentTimeMillis()

        val response = chain.proceed(request)

        Log.d("QuickHttp"," ")
        Log.d("QuickHttp","----Uploading---")
        Log.d("QuickHttp","----url        = " + request.url.toString())

        val resultStr = try {
            String(response.body!!.bytes())
        } catch (O_O: OutOfMemoryError) {
            "内存溢出"
        }

        if (request.method == "POST")
            Log.d("QuickHttp",String.format("----params     = %s", LoggingInterceptor.parseRequest(request)))

        Log.d("QuickHttp",String.format("----result     = %s", resultStr))
        Log.d("QuickHttp",String.format("----Response---- %d ms", System.currentTimeMillis()-startTime))
        Log.d("QuickHttp"," ")

        return response.newBuilder()
//                .body(ResponseBody.create(QuickHttp.mediaTypeJson, resultStr))
                .build()
        return response
    }
}