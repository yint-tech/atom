package cn.iinti.atom.system;

import cn.iinti.atom.BuildInfo;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SystemConfiguration {
    @Bean
    public PaginationInnerInterceptor paginationInterceptor() {
        return new PaginationInnerInterceptor();
    }

    @Bean
    public OpenAPI springShopOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Atom")
                        .description("atom系统")
                        .version("v" + BuildInfo.versionName)
                        .license(new License().name("Apache 2.0")
                                .url("https://atom.iinti.cn/atom-doc")))
                .externalDocs(new ExternalDocumentation()
                        .description("iinti")
                        .url("https://iinti.cn"));
    }

}