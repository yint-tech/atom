-keep @proguard.annotation.Keep class * {*;}

-keep class cn.iinti.atom.mapper.** {*;}
-keep class cn.iinti.atom.entity.** {*;}
-keep @cn.iinti.atom.service.base.metric.mql.func.MQLFunction$MQL_FUNC class * {*;}
# mbp
-keep class com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean {*;}
-keep class com.baomidou.mybatisplus.extension.ddl.IDdl
-keep class com.baomidou.mybatisplus.extension.plugins.inner.**{*;}
-keep class com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor{*;}

# spring相关
-keep @org.springframework.web.bind.annotation.RestController class * {*;}
-keep @org.springframework.context.annotation.Configuration class * {*;}
-keep @org.springframework.stereotype.Component class * {*;}
-keep @org.springframework.stereotype.Service class * {*;}


# 也是 spring 依赖,  调试时可以加上： SourceFile,LineNumberTable
-keepattributes Signature,*Annotation*

-dontwarn
-dontnote
# 有很多compileOnly级别的依赖，忽略他避免混淆中断
-ignorewarnings

-flattenpackagehierarchy cn.iinti.atom.0O