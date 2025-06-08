package cn.iinti.katom.entity.type

import com.alibaba.fastjson.JSONObject
import org.apache.commons.lang3.StringUtils
import org.apache.ibatis.type.BaseTypeHandler
import org.apache.ibatis.type.JdbcType
import org.apache.ibatis.type.MappedJdbcTypes
import org.apache.ibatis.type.MappedTypes
import java.sql.CallableStatement
import java.sql.PreparedStatement
import java.sql.ResultSet

@MappedJdbcTypes(JdbcType.VARCHAR)
@MappedTypes(JSONObject::class)
class JSONTypeHandler : BaseTypeHandler<JSONObject>() {
    override fun setNonNullParameter(ps: PreparedStatement, i: Int, parameter: JSONObject, jdbcType: JdbcType) {
        ps.setObject(i, JSONObject.toJSONString(parameter))
    }

    private fun toJson(jsonText: String?): JSONObject? {
        if (StringUtils.isBlank(jsonText)) {
            return null
        }
        return JSONObject.parseObject(jsonText)
    }

    override fun getNullableResult(rs: ResultSet, columnName: String): JSONObject? {
        val json = rs.getString(columnName)
        return toJson(json)
    }

    override fun getNullableResult(rs: ResultSet, columnIndex: Int): JSONObject? {
        val json = rs.getString(columnIndex)
        return toJson(json)
    }

    override fun getNullableResult(cs: CallableStatement, columnIndex: Int): JSONObject? {
        return cs.getObject(columnIndex, JSONObject::class.java)
    }
}