package cn.iinti.atom.system

import cn.iinti.atom.BuildInfo
import jakarta.annotation.Resource
import jakarta.servlet.http.HttpServletRequest
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.resource.PathResourceResolver
import org.springframework.web.servlet.resource.ResourceResolverChain

/**
 * Date: 2021-06-05
 *
 * @author alienhe
 */
@Configuration
class WebConfig : WebMvcConfigurer {
    @Resource
    private val loginInterceptor: LoginInterceptor? = null

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(loginInterceptor)
    }

    override fun addViewControllers(registry: ViewControllerRegistry) {
        registry.addRedirectViewController(docPath, docPath + "/index.html")
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler(docPath + "/")
            .addResourceLocations("classpath:/static/")
            .resourceChain(true)
            .addResolver(object : PathResourceResolver() {
                override fun resolveResource(
                    request: HttpServletRequest,
                    requestPath: String,
                    locations: List<org.springframework.core.io.Resource>,
                    chain: ResourceResolverChain
                ): org.springframework.core.io.Resource {
                    var requestPath = requestPath
                    return super.resolveResource(
                        request,
                        if (requestPath.endsWith("/")) "index.html" else "/index.html".let { requestPath += it; requestPath },
                        locations,
                        chain
                    )
                }
            })
        // 特殊处理文档静态资源规则，因为文档存在多个二级的index页面
        // 但是在spring里面只有root index会走welcome html
        registry.addResourceHandler(docPath + "/**")
            .addResourceLocations("classpath:/static" + docPath + "/")
            .resourceChain(true).addResolver(object : PathResourceResolver() {
                override fun resolveResource(
                    request: HttpServletRequest, requestPath: String,
                    locations: List<org.springframework.core.io.Resource>,
                    chain: ResourceResolverChain
                ): org.springframework.core.io.Resource {
                    val resource = super.resolveResource(request, requestPath, locations, chain)
                    if (resource != null) {
                        return resource
                    }
                    return super.resolveResource(request, "$requestPath/index.html", locations, chain)
                }
            })
    }

    companion object {
        private const val docPath = "/" + BuildInfo.docPath
    }
}
