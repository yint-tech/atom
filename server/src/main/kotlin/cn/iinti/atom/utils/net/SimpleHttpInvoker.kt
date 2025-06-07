package cn.iinti.atom.utils.net

import com.alibaba.fastjson.JSONObject
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.springframework.util.CollectionUtils
import java.io.IOException
import java.io.InputStream
import java.net.*
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.zip.GZIPInputStream

object SimpleHttpInvoker {
    // 请求参数
    // 请求头部

    private val threadLocalRequestHeader = ThreadLocal<LinkedHashMap<String, String>>()
    private val threadLocalProxy = ThreadLocal<String>()
    private val threadLocalProxyType = ThreadLocal<Proxy.Type>()
    private val threadLocalDefaultHttpProperty = ThreadLocal<RequestBuilder.DefaultHttpPropertyBuilder>()
    private val threadLocalConnectionTimeout = ThreadLocal<Int>()
    private val threadLocalReadTimeout = ThreadLocal<Int>()

    // 响应参数
    private val threadLocalResponseStatus = ThreadLocal<Int>()
    private val threadLocalResponseHeader = ThreadLocal<LinkedHashMap<String, String>>()
    private val threadLocalResponseIOException = ThreadLocal<IOException>()


    fun get(url: String): String {
        return asString(execute("GET", url, null)) ?: ""
    }

    fun get(url: String, param: Map<String, String>): String {
        return if (CollectionUtils.isEmpty(param)) {
            get(url)
        } else {
            val encodeParam = encodeURLParam(param)
            val fullUrl = if (url == "?") "$url$encodeParam" else "$url?$encodeParam"
            get(fullUrl)
        }
    }


    fun post(url: String, body: JSONObject): String {
        addHeader("Content-Type", "application/json; charset=UTF-8")
        return asString(execute("POST", url, body.toJSONString().toByteArray(StandardCharsets.UTF_8))) ?: ""
    }


    fun getIoException(): IOException? {
        return threadLocalResponseIOException.get()
    }

    fun encodeURLParam(params: Map<String, String>): String {
        val sb = StringBuilder()
        for ((key, value) in params) {
            sb.append(URLEncoder.encode(key, StandardCharsets.UTF_8))
                .append("=")
            if (value.isNotEmpty()) {
                sb.append(URLEncoder.encode(value, StandardCharsets.UTF_8))
            }
            sb.append("&")
        }
        if (sb.isNotEmpty()) {
            sb.deleteCharAt(sb.length - 1)
        }
        return sb.toString()
    }

    fun setProxy(ip: String, port: Int?) {
        threadLocalProxy.set("$ip:$port)")
    }

    fun setProxyAuth(userName: String, password: String) {
        GlobalAuthentication.setProxyAuth(userName, password)
    }

    fun setProxyType(proxyType: Proxy.Type?) {
        threadLocalProxyType.set(proxyType)
    }

    fun setTimout(connectTimout: Int, readTimeout: Int) {
        threadLocalConnectionTimeout.set(connectTimout)
        threadLocalReadTimeout.set(readTimeout)
    }

    fun addHeader(headers: Map<String, String>) {
        for ((key, value) in headers) {
            addHeader(key, value)
        }
    }

    fun addHeader(key: String, value: String) {
        val map = threadLocalRequestHeader.get() ?: LinkedHashMap<String, String>().apply {
            threadLocalRequestHeader.set(this)
        }
        map[key.toLowerCase()] = value
    }

    fun withIOException(ioException: IOException?) {
        threadLocalResponseIOException.set(ioException)
    }

    fun getResponseStatus(): Int {
        return threadLocalResponseStatus.get() ?: -1
    }

    fun getResponseHeader(): LinkedHashMap<String, String> {
        return threadLocalResponseHeader.get() ?: LinkedHashMap()
    }

    fun getResponseHeader(key: String): String? {
        return getResponseHeader()[key.toLowerCase()]
    }

    fun setupDefaultHttpProperty(defaultHttpPropertyBuilder: RequestBuilder.DefaultHttpPropertyBuilder?) {
        threadLocalDefaultHttpProperty.set(defaultHttpPropertyBuilder)
        defaultHttpPropertyBuilder?.build(newBuilder())
    }

    fun execute(method: String, url: String, body: ByteArray?): ByteArray? {
        // reset response
        threadLocalResponseStatus.remove()
        threadLocalResponseHeader.remove()
        threadLocalResponseIOException.remove()

        // prepare proxy
        var connection: HttpURLConnection
        try {
            val urlMode = URL(url)
            connection = if (threadLocalProxy.get() != null && threadLocalProxy.get()!!.isNotEmpty()) {
                // 有代理配置
                val type = threadLocalProxyType.get() ?: Proxy.Type.HTTP
                val ipAndPort = threadLocalProxy.get()!!.trim().split(":")
                val proxy = Proxy(type, InetSocketAddress(ipAndPort[0].trim(), ipAndPort[1].toInt()))
                urlMode.openConnection(proxy) as HttpURLConnection
            } else {
                // 没有代理
                urlMode.openConnection() as HttpURLConnection
            }

            connection.requestMethod = method

            val connectionTimeout = threadLocalConnectionTimeout.get() ?: 30000
            connection.connectTimeout = connectionTimeout

            val readTimeout = threadLocalReadTimeout.get() ?: 30000
            connection.readTimeout = readTimeout

            // fill http header
            val requestHeaders = threadLocalRequestHeader.get()
            if (requestHeaders != null) {
                for ((key, value) in requestHeaders) {
                    connection.setRequestProperty(key, value)
                }
            }

            if (body != null) {
                connection.doOutput = true
                connection.outputStream.use { os ->
                    os.write(body)
                }
            }
            return readResponse(connection)
        } catch (e: IOException) {
            threadLocalResponseIOException.set(e)
            return null
        } finally {
            // clear request
            threadLocalRequestHeader.remove()
            threadLocalProxy.remove()
            threadLocalProxyType.remove()

            val builder = threadLocalDefaultHttpProperty.get()
            builder?.build(newBuilder())
        }
    }

    fun asString(data: ByteArray?): String? {
        if (data == null) {
            return null
        }
        val headers = threadLocalResponseHeader.get() ?: LinkedHashMap()
        var charset = StandardCharsets.UTF_8

        //content-type: application/json
        val contentType = headers["content-type"]
        if (contentType != null && contentType.contains(":")) {
            val charsetStr = contentType.split(":")[1].trim()
            charset = Charset.forName(charsetStr)
        }
        return String(data, charset)
    }

    @Throws(IOException::class)
    private fun readResponse(connection: HttpURLConnection): ByteArray? {
        threadLocalResponseStatus.set(connection.responseCode)
        val responseHeader = LinkedHashMap<String, String>()
        for (i in 0..128) {
            val key = connection.getHeaderFieldKey(i) ?: continue
            responseHeader[key.toLowerCase()] = connection.getHeaderField(key)
        }
        threadLocalResponseHeader.set(responseHeader)

        return try {
            connection.inputStream.use { inputStream ->
                val contentEncoding = responseHeader["Content-Encoding"]
                val isStream: InputStream = if (StringUtils.equalsIgnoreCase(contentEncoding, "gzip")) {
                    GZIPInputStream(inputStream)
                } else {
                    inputStream
                }
                IOUtils.toByteArray(isStream)
            }
        } finally {
            connection.disconnect()
        }
    }

    fun newBuilder(): RequestBuilder {
        return RequestBuilder()
    }
}

class RequestBuilder {
    fun setProxy(ip: String, port: Int?): RequestBuilder {
        SimpleHttpInvoker.setProxy(ip, port)
        return this
    }

    fun setProxyType(proxyType: Proxy.Type?): RequestBuilder {
        SimpleHttpInvoker.setProxyType(proxyType)
        return this
    }

    fun addHeader(headers: Map<String, String>): RequestBuilder {
        SimpleHttpInvoker.addHeader(headers)
        return this
    }

    fun addHeader(key: String, value: String): RequestBuilder {
        SimpleHttpInvoker.addHeader(key, value)
        return this
    }

    fun setProxyAuth(userName: String, password: String): RequestBuilder {
        GlobalAuthentication.setProxyAuth(userName, password)
        return this
    }

    fun setTimout(connectTimout: Int, readTimeout: Int): RequestBuilder {
        SimpleHttpInvoker.setTimout(connectTimout, readTimeout)
        return this
    }

    interface DefaultHttpPropertyBuilder {
        fun build(requestBuilder: RequestBuilder)
    }
}