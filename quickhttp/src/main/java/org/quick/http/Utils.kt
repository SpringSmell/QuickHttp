package org.quick.http

import android.app.Activity
import android.os.*
import android.text.TextUtils
import android.util.Log
import com.squareup.moshi.Moshi
import org.quick.http.callback.OnWriteListener
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.Executors


object Utils {
    val saveSDCardPath = Environment.getExternalStorageDirectory().absolutePath + File.separator + "QuickAndroid"
    const val MAX_SKIP_BUFFER_SIZE = 2048/*最大缓冲区大小*/

    val mainHandler: Handler by lazy { return@lazy Handler(Looper.getMainLooper()) }
    private val executorService = Executors.newFixedThreadPool(50)

    val moshi = Moshi.Builder().build()

    inline fun <reified T> parseFromJson(json: String?): T? = try {
        val jsonAdapter = moshi.adapter<T>(T::class.java)
        jsonAdapter.fromJson(json)
    } catch (O_O: Exception) {
        Log.e("Gson", "json or class error , from  " + T::class.java.simpleName + " error json :" + json)
        null
    }

    /**
     * 将json解析成java对象
     *
     * @param json
     * @param cls
     * @return
     */
    fun <T> parseFromJson(json: String?, cls: Class<T>): T? = try {
        val jsonAdapter = moshi.adapter<T>(cls)
        jsonAdapter.fromJson(json)
    } catch (ex: Exception) {
        ex.printStackTrace()
        Log.e("Gson", "json or class error , from  " + cls.simpleName + " error json :" + json)
        null
    }


    fun runOnUiThread(onListener: () -> Unit) {
        mainHandler.post { onListener.invoke() }
    }

    /**
     * 异常线程处理数据，然后返回值
     */
    fun <T> async(onASyncListener: OnASyncListener<T>) = executorService.submit {
        try {
            val value = onASyncListener.onASync()
            runOnUiThread { onASyncListener.onAccept(value) }
        } catch (O_O: Exception) {
            runOnUiThread { onASyncListener.onError(O_O) }
        }
    }

    fun getFileName(url: String): String {
        var fileName = System.currentTimeMillis().toString()
        val endIndex = url.lastIndexOf('/')
        if (endIndex != -1 && url.length != endIndex + 1) {
            fileName = url.substring(endIndex + 1, url.length)
            val endIndex = fileName.lastIndexOf('?')
            if (endIndex != -1) fileName = fileName.substring(0, endIndex)
        }
        return fileName
    }

    fun checkActivityIsRunning(activity: Activity?): Boolean =
        !(activity == null || activity.isFinishing || Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed)

    /**
     * 格式化Get网络请求的URL
     *
     * @param url
     * @param bundle
     */
    fun formatGet(url: String, bundle: Bundle?): String {
        val params = formatParamsGet(bundle)
        return if (params.isNotEmpty()) String.format("%s?%s", url, params) else url
    }

    fun formatParamsGet(bundle: Bundle?): String {
        var params = ""
        bundle?.keySet()?.forEach {
            params += it + '=' + bundle.get(it).toString() + '&'
        }

        if (params.length > 1) params = params.substring(0, params.length - 1)

        return params
    }

    /**
     * 网络链接是否正确
     * The http link is correct or not
     *
     * @param url
     * @return
     */
    fun isHttpUrlFormRight(url: String?): Boolean {
        var url = url
        if (!TextUtils.isEmpty(url)) {
            url = url!!.toLowerCase()
            if (url.startsWith("http://") || url.startsWith("https://")) {
                return true
            }
        }
        return false
    }

    /**
     * 写入文件
     *
     * @param inputStream
     * @param filePathDir   文件路径
     * @param fileName      文件名
     * @param isAppend 是否追加文件
     */
    fun writeFile(
        inputStream: InputStream?,
        filePathDir: String,
        fileName: String,
        isAppend: Boolean,
        onWriteListener: OnWriteListener
    ) {
        if (inputStream != null) {

            async(object : OnASyncListener<File> {
                override fun onASync(): File {
                    val dir = File(filePathDir)
                    if (!dir.exists())
                        dir.mkdirs()
                    val filePath = filePathDir + File.separatorChar + fileName
                    val file = File(filePath)

                    val buffer = ByteArray(MAX_SKIP_BUFFER_SIZE)
                    val totalCount = inputStream.available().toLong()/*如果inputStream是网络流，此处数据将不准确*/
                    var redCount = 0L
                    var redBytesCount = inputStream.read(buffer)
                    val fileOutputStream = FileOutputStream(filePath, isAppend)

                    var lastTime = 0L
                    var tempTime: Long

                    var isDone = false

                    while (redBytesCount != -1) {
                        redCount += redBytesCount.toLong()
                        fileOutputStream.write(buffer, 0, redBytesCount)
                        redBytesCount = inputStream.read(buffer)

                        tempTime = System.currentTimeMillis() - lastTime
                        if (tempTime > 100L) {/*间隔80毫秒触发一次，否则队列阻塞*/
                            if (!isDone) {/*完成后只触发一次*/
                                isDone = redBytesCount == -1
                                runOnUiThread {
                                    onWriteListener.onLoading(
                                        fileName,
                                        redCount,
                                        totalCount,
                                        isDone
                                    )
                                }
                            }
                            lastTime = System.currentTimeMillis()
                        }
                    }
                    if (!isDone) runOnUiThread {
                        onWriteListener.onLoading(
                            fileName,
                            redCount,
                            totalCount,
                            true
                        )
                    }/*之前有延迟100毫秒，或许会造成最后一次未返回*/
                    fileOutputStream.close()
                    inputStream.close()
                    return file
                }

                override fun onError(O_O: Exception) {
                    onWriteListener.onFailure(O_O as IOException)
                }

                override fun onAccept(value: File) {
                    onWriteListener.onResponse(value)
                }

            })

        } else {
            throw NullPointerException()
        }
    }

    interface OnASyncListener<T> : Consumer<T> {
        fun onASync(): T
        fun onError(O_O: Exception) {}
    }

    interface Consumer<T> {
        /**
         * Consume the given value.
         * @param value the value
         * @throws Exception on error
         */
        @Throws(Exception::class)
        fun onAccept(value: T)
    }
}