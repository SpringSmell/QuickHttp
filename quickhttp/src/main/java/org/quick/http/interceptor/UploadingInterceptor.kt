package org.quick.http.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import org.quick.http.Utils

/**
 * 上传拦截器
 */
class UploadingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.currentTimeMillis()

        val response = chain.proceed(request)

        Utils.println(request)
        Utils.println(String.format("----Response---- %d ms", System.currentTimeMillis() - startTime))

        return response
    }
}