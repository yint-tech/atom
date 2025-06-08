package cn.iinti.katom.system

import jakarta.servlet.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import java.io.IOException
import java.util.zip.GZIPInputStream


@Component
class GZIPFilter : Filter {
    @Throws(IOException::class, ServletException::class)
    override fun doFilter(
        servletRequest: ServletRequest,
        servletResponse: ServletResponse,
        filterChain: FilterChain
    ) {
        val httpServletRequest = servletRequest as HttpServletRequest
        val encodeType = httpServletRequest.getHeader(CONTENT_ENCODING)
        if (CONTENT_ENCODING_TYPE.equals(encodeType, ignoreCase = true)) {
            val unZIPRequestWrapper = UnZIPRequestWrapper(httpServletRequest)
            filterChain.doFilter(unZIPRequestWrapper, servletResponse)
        } else {
            filterChain.doFilter(servletRequest, servletResponse)
        }
    }

    @Bean
    fun gzipFilterRegistrationBean(): FilterRegistrationBean<GZIPFilter> {
        val registration = FilterRegistrationBean<GZIPFilter>()
        registration.filter = this
        registration.setName("gzipFilter")
        registration.addUrlPatterns("/*")
        registration.order = 1
        return registration
    }


    class UnZIPRequestWrapper(request: HttpServletRequest) : HttpServletRequestWrapper(request) {
        private val servletInputStreamWrapper = ServletInputStreamWrapper(request.inputStream)

        override fun getInputStream(): ServletInputStream {
            return servletInputStreamWrapper
        }
    }

    private class ServletInputStreamWrapper(private val servletInputStream: ServletInputStream) : ServletInputStream() {
        private val gzipInputStream = GZIPInputStream(servletInputStream)

        override fun isFinished(): Boolean {
            return servletInputStream.isFinished
        }

        override fun isReady(): Boolean {
            return servletInputStream.isReady
        }

        override fun setReadListener(listener: ReadListener) {
            servletInputStream.setReadListener(listener)
        }

        ///// for GZIPInputStream  /////
        @Throws(IOException::class)
        override fun read(buf: ByteArray, off: Int, len: Int): Int {
            return gzipInputStream.read(buf, off, len)
        }

        @Throws(IOException::class)
        override fun close() {
            gzipInputStream.close()
        }

        @Throws(IOException::class)
        override fun read(): Int {
            return gzipInputStream.read()
        }

        @Throws(IOException::class)
        override fun available(): Int {
            return gzipInputStream.available()
        }

        @Throws(IOException::class)
        override fun skip(n: Long): Long {
            return gzipInputStream.skip(n)
        }

        override fun markSupported(): Boolean {
            return gzipInputStream.markSupported()
        }

        override fun mark(readlimit: Int) {
            gzipInputStream.mark(readlimit)
        }

        @Throws(IOException::class)
        override fun reset() {
            gzipInputStream.reset()
        }

        @Throws(IOException::class)
        override fun read(b: ByteArray): Int {
            return gzipInputStream.read(b)
        }
    }

    companion object {
        private const val CONTENT_ENCODING = "Content-Encoding"
        private const val CONTENT_ENCODING_TYPE = "gzip"
    }
}
