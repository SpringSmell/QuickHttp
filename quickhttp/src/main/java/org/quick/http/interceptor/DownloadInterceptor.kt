package org.quick.http.interceptor

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import org.quick.http.QuickHttp
import org.quick.http.ProgressResponseBody
import org.quick.http.callback.OnProgressCallback


/**
 * 下载进度监听-拦截器
 */
class DownloadInterceptor(var builder: QuickHttp.Builder, var onProgressCallback: OnProgressCallback) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.currentTimeMillis()

        val originalResponse = chain.proceed(chain.request())

        Log.d("QuickHttp"," ")
        Log.d("QuickHttp","----Download----")
        Log.d("QuickHttp","----url        = " + request.url.toString())

        if (request.method == "POST")
            Log.d("QuickHttp",String.format("----params     = %s", LoggingInterceptor.parseRequest(request)))

        Log.d("QuickHttp",String.format("----Response---- %d ms", System.currentTimeMillis()-startTime))
        Log.d("QuickHttp"," ")
        return originalResponse.newBuilder()
                .body(ProgressResponseBody(builder, originalResponse.body!!, onProgressCallback))
                .build()
    }
}