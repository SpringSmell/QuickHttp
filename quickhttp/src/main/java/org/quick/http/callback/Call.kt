package org.quick.http.callback

import org.quick.http.QuickHttp

interface Call {

    fun <T> enqueue(callback: Callback<T>) {}
    fun cancel() {}
    fun builder(): QuickHttp.Builder
}