package cn.iinti.atom

import com.baomidou.mybatisplus.generator.AutoGenerator
import com.baomidou.mybatisplus.generator.config.*
import com.baomidou.mybatisplus.generator.function.ConverterFileName
import java.io.File
import java.nio.file.Files
import java.util.*


/**
 * 此工具用于自动根据数据库生成代码，底层包装了： mybatis-plus-generator
 */
object CodeGenerator {
    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val genOutDirRoot = File(System.getProperty("user.dir"), "server/src/main/")
        val appConfigFile = File(genOutDirRoot, "resources/application.properties")
        val properties = Properties()
        properties.load(Files.newInputStream(appConfigFile.toPath()))
        val dsc = DataSourceConfig.Builder(
            properties.getProperty("spring.datasource.url"),
            properties.getProperty("spring.datasource.username"),
            properties.getProperty("spring.datasource.password")
        ).build()

        val currentModule = ""

        val pc = PackageConfig.Builder()
            .parent("cn.iinti.atom")
            .moduleName(currentModule)
            .pathInfo(object : HashMap<OutputFile?, String?>() {
                init {
                    // 默认xml生成在java代码目录，我们把他重定向到资源目录
                    val path = "$genOutDirRoot/resources/mapper/$currentModule"
                    put(OutputFile.xml, path)
                }
            })
            .build()
        val DISABLE_GEN = ConverterFileName { entityName: String? -> "" }

        val strategy = StrategyConfig.Builder()
            .addInclude(currentModule + "_.+")
            .entityBuilder().enableColumnConstant().enableLombok()
            .controllerBuilder() // disable gen Controller
            .convertFileName(DISABLE_GEN)
            .serviceBuilder() // disable gen Service
            .convertServiceFileName(DISABLE_GEN)
            .convertServiceImplFileName(DISABLE_GEN)
            .build()
        val gc = GlobalConfig.Builder()
            .enableSpringdoc()
            .disableServiceInterface()
            .outputDir("$genOutDirRoot/java")
            .author(System.getProperty("user.name"))
            .disableOpenDir()
            .build()
        val mpg = AutoGenerator(dsc)
            .strategy(strategy)
            .global(gc)
            .packageInfo(pc)
        mpg.execute()
    }
}
