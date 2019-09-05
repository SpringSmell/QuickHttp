package org.quick.http

import android.app.Activity
import android.os.*
import android.text.TextUtils
import android.util.Log
import com.squareup.moshi.Moshi
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.quick.async.Async
import org.quick.async.callback.OnASyncListener
import org.quick.http.callback.OnWriteListener
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.Executors


object Utils {
    val saveSDCardPath = Environment.getExternalStorageDirectory().absolutePath + File.separator + "QuickAndroid"
    const val MAX_SKIP_BUFFER_SIZE = 2048/*最大缓冲区大小*/

    /**
     * json类型
     */
    val mediaTypeJson: MediaType?
        get() {
            return ("application/json; charset=" + HttpService.Config.encoding).toMediaTypeOrNull()
        }
    /**
     * File类型
     */
    val mediaTypeFile: MediaType?
        get() {
            return ("application/octet-stream; charset=" + HttpService.Config.encoding).toMediaTypeOrNull()
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
        return String.format("%s?%s", url, params)
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

            Async.action(object : OnASyncListener<File> {
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
                                Async.runOnUiThread {
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
                    if (!isDone) Async.runOnUiThread {
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
}