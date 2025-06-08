package cn.iinti.katom.base.env

import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.*

private val log = LoggerFactory.getLogger(GitProperties::class.java)

object GitProperties {
    private const val CONFIG_FILE = "git.properties"
    private val properties: Properties = Properties()

    init {
        load()
    }

    class IT(val key: String, private val defaultValue: String) {
        // 从properties中加载实际值，如果不存在则使用默认值
        val value: String = properties.getProperty(key) ?: defaultValue

        companion object {
            val GIT_ID = IT("git.commit.id", "")
            val GIT_TIME = IT("git.commit.time", "")
            val GIT_USER_EMAIL = IT("git.commit.user.email", "iinti@iinti.cn")
            val GIT_BRANCH = IT("git.branch", "main")
        }
    }

    private fun load() {
        val stream = GitProperties::class.java.classLoader.getResourceAsStream(CONFIG_FILE) ?: return

        try {
            properties.load(stream)
        } catch (e: IOException) {
            log.error("load git properties error", e)
        }
    }
}