package com.example.quickhttpexample

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_fragment.*
import org.quick.http.HttpService
import org.quick.http.callback.Callback

/**
 * 测试fragment关闭了是否返回数据
 */
class FragmentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment)

        action.setOnClickListener {
            Handler().postDelayed(
                {
                    requestData()
                }, 5000
            )
        }
    }

    private fun requestData() {
/*普通请求*/
        HttpService.Builder("https://www.baidu.com/")
            .get()
            .binder(this)
            .addParams("test", "test")
            .enqueue(object : Callback<BeanKotlin>() {
                override fun onStart() {
                    Log.e("http", "开始执行")
                    super.onStart()
                }

                override fun onEnd() {
                    Log.e("http", "结束")
                    super.onEnd()
                }

                override fun onFailure(e: Throwable, isNetworkError: Boolean) {
                    e.printStackTrace()
                }

                override fun onResponse(value: BeanKotlin?) {
                    Toast.makeText(this@FragmentActivity, "收到消息", Toast.LENGTH_LONG).show()
                }
            })
    }
}