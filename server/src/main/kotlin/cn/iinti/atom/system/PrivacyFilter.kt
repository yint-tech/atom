package cn.iinti.atom.system

import cn.iinti.atom.BuildInfo
import cn.iinti.atom.service.base.config.Settings
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

@Component
class PrivacyFilter : Filter {

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        try {
            val httpServletRequest = request as HttpServletRequest
            val requestURI = httpServletRequest.requestURI
            var block = Settings.blockSwagger.value!! && isSwaggerAccess(requestURI)
            if (!block && Settings.blockActuator.value!! && isActuator(requestURI)) {
                block = true
            }
            if (!block) {
                chain.doFilter(request, response)
                return
            }
            val httpServletResponse = response as HttpServletResponse
            httpServletResponse.status = 404
        } catch (e: Exception) {
            // Ignore exception
        }
    }

    private fun isSwaggerAccess(uri: String): Boolean {
        return uri.startsWith("/v3/api-docs") || uri.startsWith("/swagger-ui/")
    }

    private fun isActuator(uri: String): Boolean {
        return uri.startsWith(BuildInfo.restfulApiPrefix + "/actuator")
    }

    @Bean
    fun privacyFilterRegistrationBean(): FilterRegistrationBean<PrivacyFilter> {
        val registration = FilterRegistrationBean<PrivacyFilter>()
        registration.filter = this
        registration.setName("privacyFilter")
        registration.addUrlPatterns("/*")
        registration.order = 1
        return registration
    }
}