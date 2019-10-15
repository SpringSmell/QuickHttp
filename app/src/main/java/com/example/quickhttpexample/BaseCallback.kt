package com.example.quickhttpexample

import com.squareup.moshi.Types
import org.quick.http.JsonUtils
import org.quick.http.callback.Callback

abstract class BaseCallback<M> : Callback<String>() {

    private fun <M> parse(json: String): M {
        val clz = tCurrentClz
        val adapter =
            if (clz.canonicalName.contains("List"))
                JsonUtils.moshi.adapter<M>(Types.newParameterizedType(List::class.java, *tTClass.toTypedArray()))
            else
                JsonUtils.moshi.adapter<M>(Types.newParameterizedType(clz, *tTClass.toTypedArray()))
        return adapter.fromJson(json) as M
    }

    override fun onResponse(value: String?) {
//        val dataList = mutableListOf<BeanJava2>()
//        for (index in 0..10) {
//            val bean = BeanJava2()
//            bean.code = 1
//            bean.msg = "这是消息$index"
//            dataList.add(bean)
//        }
//        val json = JsonUtils.parseToJson(dataList)

//        val bean = BeanJava2()
//        bean.code = 1
//        bean.msg = "这是消息1"
//        val json = JsonUtils.parseToJson(bean)
//
//        val data = BeanJava2()
//        data.msg = "这也是消息"
//        data.code = 1
//
//        val bean = BeanJava<BeanJava2>()
//        bean.code = 1
//        bean.msg = "这是消息1"
//        bean.data =data
////        val json = JsonUtils.parseToJson(bean)
//        val json2="{\"code\":1,\"msg\":\"这也是消息\",\"data\":{\"code\":1,\"msg\":\"这也是消息\"}}"

        suc(parse(value!!))
    }

    override fun onFailure(e: Throwable, isNetworkError: Boolean) {
        failed(e, isNetworkError)
    }

    abstract fun suc(value: M)
    abstract fun failed(e: Throwable, isNetworkError: Boolean)
}