//package org.quick.http
//
//import org.quick.http.callback.Call
//import org.quick.http.callback.Callback
//import org.quick.http.callback.OnDownloadListener
//import org.quick.http.callback.OnUploadingListener
//
//class QuickHttpProxy(var builder: QuickHttp.Builder) : Call {
//
//    /**
//     * 异步执行
//     */
//    override fun <T> enqueue(callback: Callback<T>) {
//        when (callback) {
//            is OnDownloadListener -> {/*下载*/
//                if (builder.isDownloadBreakpoint) {
//                    if (builder.downloadStartIndex == 0L)
//                        builder.downloadStartIndex = QuickHttp.getLocalDownloadLength(builder)
//
//                    QuickHttp.downloadBreakpoint(builder, callback)/*断点下载*/
//                } else
//                    QuickHttp.downloadGet(builder, callback)
//            }
//
//            is OnUploadingListener -> {/*上传*/
//                QuickHttp.uploadingWithJava(builder, callback)
//            }
//
//            else -> {/*普通请求*/
//                if (builder.method == "GET")
//                    QuickHttp.getWithJava(builder, callback)
//                else
//                    QuickHttp.postWithJava(builder, callback)
//            }
//        }
//    }
//
//    override fun cancel() {
//        QuickHttp.cancelTask(builder().tag)
//    }
//
//    override fun builder(): QuickHttp.Builder {
//        return builder
//    }
//
//    fun build(): Call {
//        return this
//    }
//}