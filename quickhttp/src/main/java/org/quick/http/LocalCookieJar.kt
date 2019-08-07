package org.quick.http

import android.util.SparseArray
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * 本地Cookie管理
 */
class LocalCookieJar : CookieJar {

    private val cookieStore = SparseArray<MutableList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookieStore.put(url.host.hashCode(), cookies as MutableList<Cookie>?)
    }

    override fun loadForRequest(url: HttpUrl): MutableList<Cookie> {
        return cookieStore.get(url.host.hashCode()) ?: mutableListOf()
    }
}