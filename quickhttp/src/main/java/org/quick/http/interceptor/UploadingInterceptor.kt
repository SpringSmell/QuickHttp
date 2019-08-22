package org.quick.http.interceptor

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody
import org.quick.http.HttpService
import org.quick.http.Utils

/**
 * 上传拦截器
 */
class UploadingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.currentTimeMillis()

        val response = chain.proceed(request)

        Log.d("HttpService"," ")
        Log.d("HttpService","----Uploading---")
        Log.d("HttpService","----url        = " + request.url.toString())

        val resultStr = try {
            String(response.body!!.bytes())
        } catch (O_O: OutOfMemoryError) {
            "内存溢出"
        }

        if (request.method == "POST")
            Log.d("HttpService",String.format("----params     = %s", LoggingInterceptor.parseRequest(request)))

        Log.d("HttpService",String.format("----result     = %s", resultStr))
        Log.d("HttpService",String.format("----Response---- %d ms", System.currentTimeMillis()-startTime))
        Log.d("HttpService"," ")

        return response.newBuilder()
                .body(ResponseBody.create(Utils.mediaTypeJson, resultStr))
                .build()
    }
}