package org.quick.http.interceptor

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import org.quick.http.HttpService
import org.quick.http.ProgressResponseBody
import org.quick.http.callback.OnProgressCallback


/**
 * 下载进度监听-拦截器
 */
class DownloadInterceptor(var builder: HttpService.Builder, var onProgressCallback: OnProgressCallback) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.currentTimeMillis()

        val originalResponse = chain.proceed(chain.request())

        Log.d("HttpService"," ")
        Log.d("HttpService",String.format("----Download %s---",request.method))
        Log.d("HttpService","----url        = " + request.url.toString())
        Log.d("HttpService","----header     = " + LoggingInterceptor.parseHeader(request.headers))

        if (request.method == "POST")
            Log.d("HttpService",String.format("----params     = %s", LoggingInterceptor.parseRequest(request)))

        Log.d("HttpService",String.format("----Response---- %d ms", System.currentTimeMillis()-startTime))
        Log.d("HttpService"," ")
        return originalResponse.newBuilder()
                .body(ProgressResponseBody(builder, originalResponse.body!!, onProgressCallback))
                .build()
    }
}