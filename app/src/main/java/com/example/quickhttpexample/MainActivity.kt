package com.example.quickhttpexample

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import org.quick.http.HttpService
import org.quick.http.Utils
import org.quick.http.callback.Callback
import org.quick.http.callback.OnDownloadListener
import org.quick.http.callback.OnRequestStatusCallback
import org.quick.http.callback.OnUploadingListener
import java.io.File

class MainActivity : AppCompatActivity() {

    val json = "{\n" +
            "            \"name\": \"张三\",\n" +
            "            \"age\": 16,\n" +
            "            \"sex\": true,\n" +
            "            \"mobileNum\": 15102309066\n" +
            "        }"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        config()
        onInit()
        HttpService.Builder("").get()
    }

    fun config(){
        HttpService.Config
            .baseUrl("https://www.baseurl.com")/*默认为空*/
            .addParams("TOKEN", "token")/*公共参数*/
            .method(true)/*默认为GET请求*/
            .addHeader("key", "value")/*公共头部参数*/
            .connectTimeout(200000)/*超时时间*/
            .encoding("UTF-8")/*编码*/
            .retryConnection(true)/*连接异常是否重试：默认为true*/
            .setOnRequestStatusCallback(object : OnRequestStatusCallback {
                override fun onFailure(e: Throwable, isNetworkError: Boolean) {

                }

                override fun onErrorParse(data: String) {

                }

            })
    }
    fun onInit() {


        tv0.setOnClickListener {
            /*普通请求*/
            HttpService.Builder("https://www.baidu.com/")
                .get()
                .enqueue(object : Callback<String>() {
                override fun onFailure(e: Throwable, isNetworkError: Boolean) {
                    e.printStackTrace()
                }

                override fun onResponse(value: String?) {

                }
            })
        }
        tv1.setOnClickListener {
            /*下载*/
            HttpService.Builder("https://dldir1.qq.com/weixin/android/weixin673android1360.apk")
                .downloadBreakpoint(false)/*断点下载：不调用此方法默认为true*/
                .enqueue(object : OnDownloadListener() {
                    override fun onStart() {
                        Log.e("HttpService", "onStart")
                    }

                    override fun onLoading(key: String, bytesRead: Long, totalCount: Long, isDone: Boolean) {
                        tv1.text = String.format("下载文件[%s]：%d/%d", key, bytesRead, totalCount)
                    }

                    override fun onFailure(e: Throwable, isNetworkError: Boolean) {
                        e.printStackTrace()
                        Log.e("HttpService", "onFailure")
                    }

                    override fun onResponse(value: File?) {
                        tv1.text = String.format("下载完成：%s", value!!.absolutePath)
                    }

                    override fun onEnd() {
                        Log.e("HttpService", "onEnd")
                    }

                })
        }
        tv2.setOnClickListener {
            /*上传*/
            HttpService.Builder("https://www.baidu.com")
                .addParams("file", File(Utils.saveSDCardPath + File.separatorChar + "weixin673android1360.apk"))
                .addParams("file2", File(Utils.saveSDCardPath + File.separatorChar + "weixin673android1360.apk"))
                .addParams("userName", "151*****066")
                .addParams("passWord", "888888")
                .enqueue(object : OnUploadingListener<BeanJava>() {

                    override fun onStart() {
                        super.onStart()
                    }

                    override fun onLoading(key: String, bytesRead: Long, totalCount: Long, isDone: Boolean) {
                        tv2.text = String.format("上传文件[%s]：%d/%d", key, bytesRead, totalCount)
//                                        Log.d("HttpService",String.format("正在上传[%s]：%s/%s", key, bytesRead.toString(), totalCount.toString()))

                        if ("file" == key && isDone) Log.e("HttpService", "file上传完成")
                        else if ("file2" == key && isDone) Log.e("HttpService", "file2上传完成")
                    }

                    override fun onResponse(value: BeanJava?) {
                        tv2.text = String.format("上传完成：%s", "value?.msg")
//                        Log.e("HttpService",value)
                    }

                    override fun onFailure(e: Throwable, isNetworkError: Boolean) {
                        Log.e("HttpService", "上传错误")
                        e.printStackTrace()
                    }

                    override fun onEnd() {
                        Log.e("HttpService", "上传结束")
                        super.onEnd()
                    }

                })
        }
    }
}
