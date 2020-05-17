package org.quick.http.ssl

import okhttp3.OkHttpClient
import java.security.*
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.*

object SSLUtils {
    fun createSSLSocketFactory(): SSLSocketFactory? {
        var ssfFactory: SSLSocketFactory? = null
        try {
            val sc = SSLContext.getInstance("SSL")
            sc.init(null, arrayOf<TrustManager>(MyTrustManager()), SecureRandom())
            ssfFactory = sc.socketFactory
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ssfFactory
    }

    @Throws(NoSuchAlgorithmException::class, KeyManagementException::class, KeyStoreException::class)
    fun ssl(builder: OkHttpClient.Builder): OkHttpClient.Builder {
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(null as KeyStore?)
        val trustManagers = trustManagerFactory.trustManagers
        check(!(trustManagers.size != 1 || trustManagers[0] !is X509TrustManager)) {
            ("Unexpected default trust managers:"
                    + Arrays.toString(trustManagers))
        }
        val trustManager = trustManagers[0] as X509TrustManager
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf<TrustManager>(trustManager), null)
        val sslSocketFactory = sslContext.socketFactory
        return builder.sslSocketFactory(sslSocketFactory, trustManager)
    }

    class MyTrustManager : X509TrustManager {
        @Throws(CertificateException::class)
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
        }

        @Throws(CertificateException::class)
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> {
            return arrayOf()
        }
    }
}