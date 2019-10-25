package org.quick.http.callback

interface OnRequestCallback {
    fun onFailure(e: Throwable, isNetworkError: Boolean)
    fun onRespone(data:String)
}