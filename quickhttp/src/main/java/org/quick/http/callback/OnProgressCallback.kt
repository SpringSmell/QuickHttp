package org.quick.http.callback

interface OnProgressCallback {
    fun onLoading(key: String, bytesRead: Long, totalCount: Long, isDone: Boolean)
}