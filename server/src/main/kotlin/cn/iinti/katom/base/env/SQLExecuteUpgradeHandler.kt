package cn.iinti.katom.base.env

import cn.iinti.katom.utils.ResourceUtil
import com.google.common.base.Splitter
import java.sql.SQLException
import javax.sql.DataSource

/**
 * 执行一段sql文件，请注意模块鲁棒性不强，sql使用"；"分割，不支持注释
 */
class SQLExecuteUpgradeHandler(private val sqlClassPath: String) {
    @Throws(SQLException::class)
    fun doDbUpgrade(dataSource: DataSource) {
        val sqlData = ResourceUtil.readText(sqlClassPath)
        dataSource.connection.use { connection ->
            val sqlStatementList: List<String> =
                ArrayList(Splitter.on(';').omitEmptyStrings().trimResults().splitToList(sqlData)) as List<String>
            for (sql in sqlStatementList) {
                connection.createStatement().use { statement ->
                    println("execute sql:$sql")
                    statement.execute(sql)
                }
            }
        }
    }
}