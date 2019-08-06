package org.quick.http.callback

interface OnRequestStatusCallback {
    fun onFailure(e: Throwable, isNetworkError: Boolean)
    fun onErrorParse(data:String)
}