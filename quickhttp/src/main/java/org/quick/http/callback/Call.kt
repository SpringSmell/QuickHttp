package org.quick.http.callback

import org.quick.http.HttpService

interface Call {

    fun <T> enqueue(callback: Callback<T>) {}
    fun cancel() {}
    fun builder(): HttpService.Builder
}