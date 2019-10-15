package org.quick.http.callback

import java.lang.Exception
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType

/**
 * 泛型Class获取
 * 解决泛型擦除的问题
 * @from https://blog.csdn.net/Fy993912_chris/article/details/84765483
 */
abstract class ClassCallback<T> {

    /**
     * 泛型Class
     * TestBean<String,Int> TestBean的Class，若无，返回Object.Class
     */
    val tClass: Class<T>
        get() = getTClass(javaClass) as Class<T>

    /**
     * 泛型中的泛型列表
     * TestBean<String,Int> String,Int的Class,若无，返回列表为空
     */
    val tTClass: ArrayList<Class<T>>
        get() = getTTClass(javaClass) as ArrayList<Class<T>>

    val tCurrentClz: Class<T>
        get() = getTCurrentClass(javaClass) as Class<T>

    companion object {

        fun getTCurrentClass(clz: Class<*>): Class<*> {
            val type = getClzType(clz)
            return try {
                type as Class<*>
            } catch (O_O: Exception) {
                Any::class.java
            }
        }


        /**
         * 获取泛型的类型
         */
        fun getTClass(clz: Class<*>): Class<*> {
            val superSuperType = getClzType(clz.superclass.superclass)
            val superType = superSuperType ?: getClzType(clz.superclass)
            val type = superType ?: getClzType(clz)
            return try {
                type as Class<*>
            } catch (O_O: Exception) {
                Any::class.java
            }
        }


        fun getClzType(clz: Class<*>): Type? {
            val type = clz.genericSuperclass as? ParameterizedType ?: return null

            val params = type.actualTypeArguments
            return when {
                params[0] is ParameterizedType ->/*泛型内还有泛型*/
                    (params[0] as ParameterizedType).rawType
                params[0] is Class<*> ->
                    params[0]
                else ->
                    null
            }
        }

        fun getTTClass(clz: Class<*>): ArrayList<Class<*>> {
            val clzArray = arrayListOf<Class<*>>()
            val type = clz.genericSuperclass as? ParameterizedType ?: return clzArray
            val params = type.actualTypeArguments
            if (params[0] is ParameterizedType) {
                when (val pType = (params[0] as ParameterizedType).actualTypeArguments[0]) {
                    is Class<*> ->
                        clzArray.add(pType)
                    is WildcardType -> {
                        if (pType.upperBounds[0] is Class<*>) {
                            clzArray.add(pType.upperBounds[0] as Class<*>)
                        }
                    }
                }
            }
            return clzArray
        }

//        /**
//         * 获取泛型中的泛型
//         */
//        fun getTTClass(clz: Class<*>): ArrayList<Class<*>> {
//            val clzArray = arrayListOf<Class<*>>()
//            val type = clz.genericSuperclass as? ParameterizedType ?: return clzArray
//            val params = type.actualTypeArguments
//            if (params[0] is ParameterizedType) {
//                val pType = (params[0] as ParameterizedType).actualTypeArguments[0]
//                when (pType) {
//                    is Class<*> ->
//                        pType
//                    is WildcardType -> {
//                        if (pType.upperBounds[0] is Class<*>)
//
//                    }
//                }
//                (params[0] as ParameterizedType).actualTypeArguments.forEach {
//                    clzArray.add(it as Class<*>)
//                }
//            }
//
//            return clzArray
//        }
    }
}