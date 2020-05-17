package com.example.quickhttpexample

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.quickhttpexample.model.BeanKotlin
import kotlinx.android.synthetic.main.fragment_test.*
import org.quick.http.HttpService
import org.quick.http.callback.Callback

class TestFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return LayoutInflater.from(activity).inflate(R.layout.fragment_test, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        action.setOnClickListener {
            requestData()
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
                    Toast.makeText(activity!!, "收到消息", Toast.LENGTH_LONG).show()
                }
            })
    }
}