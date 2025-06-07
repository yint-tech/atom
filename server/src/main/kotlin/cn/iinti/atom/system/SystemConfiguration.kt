package cn.iinti.atom.system

import cn.iinti.atom.BuildInfo
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor
import io.swagger.v3.oas.models.ExternalDocumentation
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.parameters.QueryParameter
import org.apache.commons.lang3.StringUtils
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SystemConfiguration {
    @Bean
    fun paginationInterceptor(): PaginationInnerInterceptor {
        return PaginationInnerInterceptor()
    }

    @Bean
    fun springShopOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Atom")
                    .description("atom系统")
                    .version("v${BuildInfo.versionName}")
                    .license(
                        License()
                            .name("Apache 2.0")
                            .url("https://atom.iinti.cn/atom-doc")
                    )
            )
            .externalDocs(
                ExternalDocumentation()
                    .description("iinti")
                    .url("https://iinti.cn")
            )
    }

    @Bean
    fun apiCustomizer(): OperationCustomizer {
        return OperationCustomizer { operation, handlerMethod ->
            val loginRequired = handlerMethod.method.getAnnotation(LoginRequired::class.java)
                ?: return@OperationCustomizer operation
            val newSummary = ArrayList<String>()
            if (loginRequired.forAdmin) {
                newSummary.add("AdminOnly")
            }
            if (loginRequired.apiToken) {
                newSummary.add("SupportApiToken")
            }
            val summary = operation.summary
            if (StringUtils.isNotBlank(summary)) {
                newSummary.add(summary)
            }
            operation.summary = StringUtils.join(newSummary, " ")
            val parameter = QueryParameter()
                .name(BuildInfo.userLoginTokenKey)
                .description("接口Token")
                .required(true)
                .allowEmptyValue(false)
            operation.addParametersItem(parameter)

            operation
        }
    }
}