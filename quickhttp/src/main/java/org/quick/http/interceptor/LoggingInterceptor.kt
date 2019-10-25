package org.quick.http.interceptor

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
        Utils.println(request)
        Utils.println(String.format("----Response---- %d ms", System.currentTimeMillis() - startTime))
        return response
    }

    companion object {
        fun parseRequest(request: Request): String {
            var params = ""
            when (val requestBody = request.body) {
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

            return String.format(
                "{ %s }",
                if (params.length > 2) params.substring(0, params.length - 2) else params
            )
        }

        fun parseHeader(headers: Headers): String {
            var params = ""
            headers.forEach {
                params += it.first + " = " + it.second + " , "
            }
            return String.format(
                "{ %s }",
                if (params.length > 2) params.substring(0, params.length - 2) else params
            )
        }
    }
}