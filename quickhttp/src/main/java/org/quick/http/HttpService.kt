@file:Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "UNCHECKED_CAST")

package org.quick.http

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import androidx.fragment.app.Fragment
import okhttp3.*
import org.quick.async.Async
import org.quick.http.Utils.mediaTypeFile
import org.quick.http.callback.OnDownloadListener
import org.quick.http.callback.OnUploadingListener
import org.quick.http.callback.OnWriteListener
import org.quick.http.interceptor.DownloadInterceptor
import org.quick.http.interceptor.LoggingInterceptor
import org.quick.http.interceptor.UploadingInterceptor
import java.io.File
import java.io.IOException
import java.net.ConnectException
import java.security.KeyStore
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * 网络服务
 * 使用okHttp
 * @from https://github.com/SpringSmell/HttpService
 */
object HttpService {
    const val TAG = "HttpService"
    private val taskCalls = HashMap<String, Call>()

    /**
     * Cookie管理
     */
    private val localCookieJar = LocalCookieJar()

    /**
     * 上一次的JSON
     */
    private var lastJsonSa = HashMap<String, String>()

    private val clientBuilder by lazy {
        return@lazy OkHttpClient.Builder()
            .connectTimeout(Config.connectTimeout, TimeUnit.SECONDS)
            .readTimeout(Config.readTimeout, TimeUnit.SECONDS)
            .writeTimeout(Config.writeTimeout, TimeUnit.SECONDS)
            .retryOnConnectionFailure(Config.isRetryConnection)
            .cookieJar(localCookieJar)
            .cache(Cache(File(Config.cachePath), Config.cacheSize))
            .followRedirects(true)
    }
    val normalClient by lazy {
        clientBuilder
            .addInterceptor(LoggingInterceptor())
            .hostnameVerifier(HostnameVerifier { hostname, session -> true })
            .apply {
                val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                trustManagerFactory.init(null as KeyStore?)
                val trustManagers = trustManagerFactory.trustManagers
                check(!(trustManagers.size != 1 || trustManagers[0] !is X509TrustManager)) {
                    ("Unexpected default trust managers:" + Arrays.toString(trustManagers))
                }
                val trustManager = trustManagers[0] as X509TrustManager
                val sslContext = SSLContext.getInstance("SSL")
                sslContext.init(null, arrayOf<TrustManager>(trustManager), null)
                val sslSocketFactory = sslContext.socketFactory
                sslSocketFactory(sslSocketFactory, trustManager)
            }
            .build()
    }

    val downloadClient by lazy {
        clientBuilder.build()
    }

    val uploadingClient by lazy {
        clientBuilder
            .addInterceptor(UploadingInterceptor())
            .build()
    }

    /**
     * 构建保存请求体的Key
     */
    private fun buildKey(builder: Builder, request: Request): String {
        var key =
            if (request.method == "GET")
                request.url.toString()
            else
                LoggingInterceptor.parseRequest(request)

        builder.context?.run {
            key = javaClass.simpleName + key
        }

        builder.fragment?.run {
            key = javaClass.simpleName + key
        }
        return key
    }

    /**
     * 构建通用的Call
     */
    private fun getCall(client: OkHttpClient, request: Request, builder: Builder): Call {
        val call = client.newCall(request)
        taskCalls[buildKey(builder, request)] = call
        return call
    }

    /**
     * 移除请求队列
     */
    private fun removeTask(builder: Builder, request: Request) {
        taskCalls.remove(buildKey(builder, request))
    }

    /**
     * 根据参数获取requestBody
     */
    private fun getRequestBody(builder: Builder): RequestBody {
        return run {
            val formBody = FormBody.Builder()
            builder.requestBodyBundle.keySet().forEach {
                formBody.add(it, builder.requestBodyBundle.get(it).toString())
            }
            if (builder.isSendPublicKey)
                Config.params.keySet().forEach {
                    formBody.add(it, Config.params.get(it).toString())
                }
            formBody.build()
        }
    }

    /**
     * 构建Get请求，并添加默认参数
     */
    private fun getRequest(builder: Builder): Request.Builder {
        val url = when {
            !builder.isSendPublicKey -> {
                Utils.formatGet(configUrl(builder.url), builder.requestBodyBundle)
            }
            builder.requestBodyBundle.size() > 0 -> {
                if (Config.params.size() > 0) {
                    Utils.formatGet(configUrl(builder.url), builder.requestBodyBundle) + '&' + Utils.formatParamsGet(Config.params)
                } else
                    Utils.formatGet(configUrl(builder.url), builder.requestBodyBundle)
            }
            Config.params.size() > 0 -> {
                Utils.formatGet(configUrl(builder.url), Config.params)
            }
            else ->
                Utils.formatGet(configUrl(builder.url), builder.requestBodyBundle)
        }

        val request = Request.Builder().url(url).tag(builder.tag)
        builder.header.keySet().forEach { request.addHeader(it, builder.header.get(it).toString()) }

        if (builder.isSendPublicKey)
            Config.header.keySet().forEach { request.addHeader(it, Config.header.get(it).toString()) }

        if (builder.isDownloadBreakpoint && builder.downloadEndIndex != 0L)
            request.addHeader("RANGE", String.format("bytes=%d-%d", builder.downloadStartIndex, builder.downloadEndIndex))

        return request
    }

    /**
     * 构建Post请求，并添加默认参数
     */
    private fun postRequest(builder: Builder): Request.Builder {
        val request =
            Request.Builder()
                .url(configUrl(builder.url))
                .tag(builder.tag)
                .post(getRequestBody(builder))
        builder.header.keySet().forEach { request.addHeader(it, builder.header.get(it).toString()) }
        if (builder.isSendPublicKey)
            Config.header.keySet().forEach { request.addHeader(it, Config.header.get(it).toString()) }
        if (builder.isDownloadBreakpoint && builder.downloadEndIndex != 0L)
            request.addHeader("RANGE", String.format("bytes=%d-%d", builder.downloadStartIndex, builder.downloadEndIndex))
        return request
    }

    /**
     * 配置URL
     */
    private fun configUrl(postfix: String): String {

        return if (Utils.isHttpUrlFormRight(postfix.trim()))
            postfix.trim()
        else
            Config.baseUrl + postfix.trim()
    }

    /**
     * 异常检测
     */
    private fun checkOOM(response: Response): String {
        return try {
            response.body!!.string()
        } catch (O_O: OutOfMemoryError) {/*服务器返回数据太大，会造成OOM，比如返回APK，高清图片*/
            "内存溢出\n" + O_O.message
        }
    }

    /**
     * 绑定者是否生存
     */
    private fun checkBinderIsExist(builder: Builder): Boolean = when {
        builder.context != null && builder.context is Activity ->
            Utils.checkActivityIsRunning(builder.context as Activity)
        builder.fragment != null ->
            /*所依赖的Activity还在运行中*/
            (builder.fragment!!.activity != null && Utils.checkActivityIsRunning(builder.fragment!!.activity))
                    &&
                    (builder.fragment!!.isAdded && !builder.fragment!!.isDetached)/*已经添加到Activity中，并且没有被分离出来*/
        else ->
            true
    }

    private fun <T> onFailure(call: Call, e: IOException, builder: Builder, callback: org.quick.http.callback.Callback<T>) {
        removeTask(builder, call.request())
        if (checkBinderIsExist(builder)) {
            Async.runOnUiThread {
                callback.onFailure(e, e.javaClass == ConnectException::class.java)
                Config.onFailedCallback?.invoke(e, e.javaClass == ConnectException::class.java)
                callback.onEnd()
            }
        }
    }

    private fun <T> onResponse(call: Call, response: Response, builder: Builder, callback: org.quick.http.callback.Callback<T>) {
        removeTask(builder, call.request())
        val data = checkOOM(response)
        Config.onResponseCallback?.invoke(data)
        if (checkBinderIsExist(builder)) {
            Async.runOnUiThread {
                if (builder.ignoreEqualJson) {
                    if (lastJsonSa[builder.url] == data) {
                        callback.onEnd()
                        Utils.println("已忽略相同的JSON：$data")
                        return@runOnUiThread
                    } else
                        lastJsonSa[builder.url] = data
                }

                val model: T? =
                    if (callback.tClass == String::class.java)
                        data as T
                    else {
                        JsonUtils.parseFromJson(data, callback.tClass, *callback.tTClass.toTypedArray())
                    }
                val result = if (model != null) JsonUtils.parseToJson(model) else data
                Utils.printJson(result)
                callback.onResponse(model)
                callback.onEnd()
            }
        } else if (Config.isDebug)
            Utils.println("所依赖的绑定者已销毁")
    }

    /**
     * 兼容java
     * 返回json使用GSON解析，若 T = String 则不进行解析
     */
    private fun <T> getWithJava(builder: Builder, callback: org.quick.http.callback.Callback<T>) {
        val request = getRequest(builder).build()

        callback.onStart()
        getCall(normalClient, request, builder)
            .enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    onFailure(call, e, builder, callback)
                }

                override fun onResponse(call: Call, response: Response) {
                    onResponse(call, response, builder, callback)
                }
            })

    }

    /**
     * 兼容java
     * 返回值json使用GSON解析，若 T = String 则不进行解析
     */
    private fun <T> postWithJava(builder: Builder, callback: org.quick.http.callback.Callback<T>) {
        val request = postRequest(builder).build()

        callback.onStart()
        getCall(normalClient, request, builder).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onFailure(call, e, builder, callback)
            }

            override fun onResponse(call: Call, response: Response) {
                onResponse(call, response, builder, callback)
            }
        })
    }

    /**
     * 兼容java
     * 上传文件
     */
    private fun <T> uploadingWithJava(builder: Builder, callback: OnUploadingListener<T>) {

        val multipartBody = MultipartBody.Builder().setType(MultipartBody.FORM)

        builder.requestBodyBundle.keySet().forEach {
            multipartBody.addFormDataPart(it, builder.requestBodyBundle.get(it).toString())
        }

        if (builder.isSendPublicKey)
            Config.params.keySet().forEach {
                multipartBody.addFormDataPart(it, Config.params.get(it).toString())
            }

        builder.fileBundle.keySet().forEach {
            when (val obj = builder.fileBundle.getSerializable(it)) {
                is File ->
                    if (obj.exists())
                        multipartBody.addFormDataPart(it, obj.name, UploadingRequestBody(mediaTypeFile!!, it, obj, callback))
                is ArrayList<*> -> {
                    obj.forEach { temp ->
                        val file = temp as File
                        if (file.exists())
                            multipartBody.addFormDataPart(it, file.name, UploadingRequestBody(Utils.mediaTypeFile!!, it, file, callback))
                    }
                }
            }
        }

        val request = Request.Builder()
            .url(configUrl(builder.url))
            .tag(builder.tag)
            .post(multipartBody.build())
        builder.header.keySet().forEach { request.addHeader(it, builder.header.get(it).toString()) }
        if (builder.isSendPublicKey)
            Config.header.keySet().forEach { request.addHeader(it, Config.header.get(it).toString()) }

        callback.onStart()
        getCall(uploadingClient, request.build(), builder).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onFailure(call, e, builder, callback)
            }

            override fun onResponse(call: Call, response: Response) {
                onResponse(call, response, builder, callback)
            }
        })
    }

    /**
     * 使用GET方式下载文件
     */
    private fun downloadGet(builder: Builder, onDownloadListener: OnDownloadListener) {
        if (!builder.isDownloadBreakpoint)/*在断点下载的方法里已经调用了onStart方法，此时忽略*/
            onDownloadListener.onStart()
        val request = getRequest(builder).build()
        getCall(clientBuilder.addNetworkInterceptor(DownloadInterceptor(builder, onDownloadListener)).build(), request, builder)
            .enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    onFailure(call, e, builder, onDownloadListener)
                }

                override fun onResponse(call: Call, response: Response) {
                    writeDownload(call, response, builder, onDownloadListener)
                }
            })
    }

    /**
     * 使用POST方式下载文件
     */
    private fun downloadPost(builder: Builder, onDownloadListener: OnDownloadListener) {
        if (!builder.isDownloadBreakpoint)/*在断点下载的方法里已经调用了onStart方法*/
            onDownloadListener.onStart()
        val request = postRequest(builder).build()
        getCall(clientBuilder.addNetworkInterceptor(DownloadInterceptor(builder, onDownloadListener)).build(), request, builder)
            .enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    onFailure(call, e, builder, onDownloadListener)
                }

                override fun onResponse(call: Call, response: Response) {
                    writeDownload(call, response, builder, onDownloadListener)
                }
            })
    }

    /**
     * 写入
     */
    private fun writeDownload(call: Call, response: Response, builder: Builder, onDownloadListener: OnDownloadListener) {
        if (checkBinderIsExist(builder))
            Utils.writeFile(
                response.body?.byteStream(),
                builder.downloadDir,
                builder.downloadFileName,
                builder.isDownloadBreakpoint,
                object : OnWriteListener {
                    override fun onLoading(
                        key: String,
                        bytesRead: Long,
                        totalCount: Long,
                        isDone: Boolean
                    ) = Unit

                    override fun onResponse(file: File) {
                        removeTask(builder, call.request())
                        if (checkBinderIsExist(builder)) {
                            onDownloadListener.onResponse(file)
                            onDownloadListener.onEnd()
                        }
                    }

                    override fun onFailure(O_O: IOException) {
                        removeTask(builder, call.request())
                        if (checkBinderIsExist(builder)) {
                            onDownloadListener.onFailure(O_O, false)
                            onDownloadListener.onEnd()
                        }
                    }
                })
    }

    /**
     * 获取本地文件的总长度
     */
    private fun getLocalDownloadLength(builder: Builder): Long {
        val file = File(builder.downloadDir + File.separatorChar + builder.downloadFileName)
        return if (file.exists()) file.length() else 0
    }

    /**
     * 断点下载文件
     */
    private fun downloadBreakpoint(builder: Builder, onDownloadListener: OnDownloadListener) {
        onDownloadListener.onStart()
        when {
            builder.downloadEndIndex == 0L ->/*没有指定下载结束*/ {
                downloadClient
                    .newCall(
                        if (builder.method == "GET")
                            getRequest(builder).build()
                        else
                            postRequest(builder).build()
                    ).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            onFailure(call, e, builder, onDownloadListener)
                        }

                        override fun onResponse(call: Call, response: Response) {
                            builder.downloadEndIndex = response.body!!.contentLength()
                            response.body?.close()
                            when {
                                builder.downloadEndIndex == builder.downloadStartIndex -> /*本地与线上一致*/
                                    Async.runOnUiThread {
                                        onDownloadListener.onResponse(File(builder.downloadDir + File.separatorChar + builder.downloadFileName))
                                        onDownloadListener.onEnd()
                                    }
                                builder.method == "GET" ->
                                    downloadGet(builder, onDownloadListener)
                                else ->
                                    downloadPost(builder, onDownloadListener)
                            }
                        }
                    })
            }
            builder.method == "GET" ->
                downloadGet(builder, onDownloadListener)
            else ->
                downloadPost(builder, onDownloadListener)
        }

    }

    /**
     * 取消指定正在运行的任务
     */
    fun cancelTask(tag: String?): Boolean {
        var isCancel = false
        if (TextUtils.isEmpty(tag))
            return isCancel

        for (call in taskCalls) {
            if (tag == call.value.request().tag() && !call.value.isCanceled()) {
                if (!call.value.isCanceled()) {
                    call.value.cancel()
                    isCancel = true
                }
                taskCalls.remove(call.key)
                break
            }
        }
        return isCancel
    }

    fun checkTask(tag: String) = taskCalls.contains(tag)

    /**
     * 取消指定正在运行的任务
     */
    fun cancelTask(activity: Activity?) {
        activity?.run {
            for (call in taskCalls) {
                if (call.key.startsWith(javaClass.simpleName)) {
                    if (!call.value.isCanceled())
                        call.value.cancel()
                }
            }
        }
    }

    /**
     * 取消指定正在运行的任务
     */
    fun cancelTask(fragment: Fragment?) {
        fragment?.run {
            for (call in taskCalls) {
                if (call.key.startsWith(javaClass.simpleName)) {
                    if (!call.value.isCanceled())
                        call.value.cancel()
                }
            }
        }
    }

    /**
     * 取消所有正在运行的任务
     */
    fun cancelAllTask() {
        for (call in taskCalls) {
            if (!call.value.isCanceled())
                call.value.cancel()
        }
        taskCalls.clear()
    }

    /**
     * 构造器
     */
    class Builder(val url: String) {

        val requestBodyBundle = Bundle()
        val fileBundle = Bundle()
        val header = Bundle()
        var isSendPublicKey = true
        var method: String = Config.defaultMethod
        var tag: String? = null
        internal var downloadStartIndex = 0L
        internal var downloadEndIndex = 0L
        internal var isDownloadBreakpoint = true/*是否断点下载*/
        internal var downloadDir: String = ""
        internal var downloadFileName: String = ""
        internal var ignoreEqualJson = false/*忽略相同的JSON*/

        var fragment: Fragment? = null
        var context: Context? = null

        fun get() =
            also { this.method = "GET" }

        fun post() =
            also { this.method = "POST" }

        /**
         * 忽略上一次相同的JSON串
         */
        fun ignoreEqualJson(isIgnore: Boolean = true) =
            also { ignoreEqualJson = isIgnore }

        /**
         * 与fragment生命周期绑定，若fragment销毁或分离，请求将不会返回
         */
        fun binder(fragment: androidx.fragment.app.Fragment?) =
            also { this.fragment = fragment }

        fun sendPublicKey(isSendPublicKey: Boolean = true) = also { this.isSendPublicKey = isSendPublicKey }

        /**
         * 与activity生命周期绑定，若activity销毁，请求将不会返回
         */
        fun binder(context: Context?) =
            also { this.context = context }

        /**
         * 添加标识，可用于取消任务
         */
        fun tag(tag: String) =
            also { this.tag = tag }

        /**
         * 添加header
         */
        fun addHeader(key: String, value: Any) =
            also { header.putString(key, value.toString()) }

        fun container(key: String) = requestBodyBundle.containsKey(key)

        fun getBodyValue(key: String) = requestBodyBundle.get(key)

        fun getHeaderValue(key: String) = header.get(key)

        /**
         * 添加参数
         */
        fun addParams(bundle: Bundle) =
            also { requestBodyBundle.putAll(bundle) }

        fun addParams(map: Map<String, *>) =
            also { map.keys.forEach { requestBodyBundle.putString(it, map[it].toString()) } }

        fun addParams(key: String, value: String) =
            also { requestBodyBundle.putString(key, value) }

        fun addParams(key: String, value: Int) =
            also { requestBodyBundle.putInt(key, value) }

        fun addParams(key: String, value: Long) =
            also { requestBodyBundle.putLong(key, value) }

        fun addParams(key: String, value: Float) =
            also { requestBodyBundle.putFloat(key, value) }

        fun addParams(key: String, value: Double) =
            also { requestBodyBundle.putDouble(key, value) }

        fun addParams(key: String, value: Boolean) =
            also { requestBodyBundle.putBoolean(key, value) }

        fun addParams(key: String, value: Char) =
            also { requestBodyBundle.putChar(key, value) }

        fun addParams(key: String, value: CharSequence) =
            also { requestBodyBundle.putCharSequence(key, value) }

        fun addParams(key: String, value: Byte) =
            also { requestBodyBundle.putByte(key, value) }

        fun addParams(key: String, value: File) =
            also { fileBundle.putSerializable(key, value) }

        fun addParams(key: String, value: ArrayList<File>) =
            also { fileBundle.putSerializable(key, value) }

        fun downloadDir(dirPath: String) =
            also { this.downloadDir = dirPath }

        fun downloadFileName(fileName: String) =
            also { this.downloadFileName = fileName }

        /**
         * 断点下载索引
         * @param startIndex 为0时：自动获取本地路径文件大小
         * @param endIndex 为0时：自动获取总大小
         */
        fun downloadBreakpointIndex(startIndex: Long, endIndex: Long) =
            also {
                this.isDownloadBreakpoint = true
                this.downloadStartIndex = startIndex
                this.downloadEndIndex = endIndex
            }

        /**
         * 断点下载
         */
        fun downloadBreakpoint(isBreakpoint: Boolean = true) =
            also { this.isDownloadBreakpoint = isBreakpoint }

        /**
         * 开始执行
         * @param callback
         * from package org.quick.http.callback
         * 1、{@link OnDownloadListener} 下载文件
         * 2、{@link OnUploadingListener}上传文件
         * 3、{@link Callback} 普通请求
         */
        fun <T> enqueue(callback: org.quick.http.callback.Callback<T>) =
            build().apply { enqueue(callback) }

        fun build(): org.quick.http.callback.Call = QuickHttpProxy(this).build()
    }

    /**
     * 公共参数配置
     */
    object Config {
        /*公共Header*/
        val header = Bundle()

        /*公共参数*/
        val params = Bundle()
        internal var baseUrl = ""
        internal var defaultMethod = "GET"
        internal var encoding = "utf-8"
        internal var readTimeout = 30L
        internal var writeTimeout = 30L
        internal var connectTimeout = 30L

        /*如果遇到连接问题，是否重试*/
        internal var isRetryConnection = true
        internal var cachePath: String = Utils.saveSDCardPath
        internal var cacheSize = 10 * 1024 * 1024L

        /*请求之前回调*/
        internal var onRequestBeforeListener: ((builder: Builder) -> Unit)? = null
        internal var onFailedCallback: ((e: IOException, isNetError: Boolean) -> Unit)? = null
        internal var onResponseCallback: ((data: String) -> Unit)? = null

        /**
         * 是否调试
         */
        internal var isDebug = false
        internal var isPrintAllJson = false


        fun printAllJson(isAll: Boolean) = also { this.isPrintAllJson = isAll }
        fun debug(isDebug: Boolean = true) = also { this.isDebug = isDebug }

        fun baseUrl(url: String) = also { this.baseUrl = url }

        /**
         * @param isGet true:GET   false:POST
         */
        fun method(isGet: Boolean) = also { defaultMethod = if (isGet) "GET" else "POST" }

        /**
         * 添加公共header
         */
        fun addHeader(key: String, value: Any) = also { header.putString(key, value.toString()) }

        fun removeHeader(key: String) = also { header.remove(key) }

        /**
         * 添加公共参数
         */
        fun addParams(key: String, value: Any) = also { params.putString(key, value.toString()) }

        fun removeParams(key: String) = also { params.remove(key) }

        /**
         * 指定编码，默认utf-8
         */
        fun encoding(encoding: String) = also { this.encoding = encoding }

        /**
         * 读取超时时间
         */
        fun readTimeout(timeout: Long) = also { this.readTimeout = timeout }

        /**
         * 写入超时时间
         */
        fun writeTimeout(timeout: Long) = also { this.writeTimeout = timeout }

        /**
         * 连接超时时间
         */
        fun connectTimeout(timeout: Long) = also { this.connectTimeout = timeout }

        /**
         * 遇到连接问题，是否重试
         */
        fun retryConnection(isRetryConnection: Boolean) = also { this.isRetryConnection = isRetryConnection }

        /**
         * 缓存路径
         */
        fun cachePath(dirPath: String) = also { this.cachePath = dirPath }

        /**
         * 每次-请求之前回调
         */
        fun onRequestBefore(listener: (builder: Builder) -> Unit) = also { this.onRequestBeforeListener = listener }

        fun onFailedCallback(onFailedCallback: (e: IOException, isNetError: Boolean) -> Unit) = also { this.onFailedCallback = onFailedCallback }

        fun onResponseCallback(onResponseCallback: (data: String) -> Unit) = also { this.onResponseCallback = onResponseCallback }
    }

    internal class QuickHttpProxy(private var builder: Builder) : org.quick.http.callback.Call {

        /**
         * 异步执行
         */
        override fun <T> enqueue(callback: org.quick.http.callback.Callback<T>) {
            Config.onRequestBeforeListener?.invoke(builder)
            when (callback) {
                is OnDownloadListener -> {/*下载*/
                    builder.run {
                        if (TextUtils.isEmpty(builder.downloadFileName))
                            builder.downloadFileName = Utils.getFileName(configUrl(builder.url))

                        if (TextUtils.isEmpty(builder.downloadDir)) {/*目录不存在，提前创建*/
                            builder.downloadDir = Config.cachePath
                            val file = File(downloadDir)
                            if (!file.exists())
                                file.mkdir()
                        }

                        when {
                            builder.isDownloadBreakpoint -> {
                                if (builder.downloadStartIndex == 0L)
                                    builder.downloadStartIndex = getLocalDownloadLength(builder)

                                downloadBreakpoint(builder, callback)/*断点下载*/
                            }
                            builder.method == "GET" ->
                                downloadGet(builder, callback)
                            else ->
                                downloadPost(builder, callback)
                        }
                    }
                }

                is OnUploadingListener -> {/*上传*/
                    uploadingWithJava(builder, callback)
                }

                else -> {/*普通请求*/
                    if (builder.method == "GET")
                        getWithJava(builder, callback)
                    else
                        postWithJava(builder, callback)
                }
            }
        }

        override fun cancel() {
            cancelTask(builder().tag)
        }

        override fun builder() = builder

        fun build() = this
    }
}