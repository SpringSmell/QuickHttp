package org.quick.http.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import org.quick.http.HttpService
import org.quick.http.ProgressResponseBody
import org.quick.http.Utils
import org.quick.http.callback.OnProgressCallback


/**
 * 下载进度监听-拦截器
 */
class DownloadInterceptor(var builder: HttpService.Builder, var onProgressCallback: OnProgressCallback) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.currentTimeMillis()

        val originalResponse = chain.proceed(chain.request())

        Utils.println(request)
        Utils.println(String.format("----Response---- %d ms", System.currentTimeMillis() - startTime))

        return originalResponse.newBuilder()
            .body(ProgressResponseBody(builder, originalResponse.body!!, onProgressCallback))
            .build()
    }
}