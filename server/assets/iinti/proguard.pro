-keep class cn.iinti.atom.mapper.** {*;}
-keep class cn.iinti.atom.entity.** {*;}
-keep @cn.iinti.atom.service.base.metric.mql.func.MQLFunction$MQL_FUNC class * {*;}

# 也是 spring 依赖,  调试时可以加上： SourceFile,LineNumberTable
-keepattributes Signature,*Annotation*

-dontwarn
-dontnote
# 有很多compileOnly级别的依赖，忽略他避免混淆中断
-ignorewarnings

-flattenpackagehierarchy cn.iinti.atom.0O