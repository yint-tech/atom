package cn.iinti.atom.service.base

import cn.iinti.atom.BuildInfo
import cn.iinti.atom.entity.CommonRes
import cn.iinti.atom.entity.UserInfo
import cn.iinti.atom.mapper.UserInfoMapper
import cn.iinti.atom.service.base.config.Settings
import cn.iinti.atom.service.base.dbcache.DbCacheManager
import cn.iinti.atom.service.base.perm.PermsService
import cn.iinti.atom.system.AppContext
import cn.iinti.atom.utils.Md5Utils
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import jakarta.annotation.Resource
import org.springframework.stereotype.Service
import java.io.IOException
import java.time.LocalDateTime
import java.time.temporal.ChronoField
import java.util.*

/**
 * <p>
 * 用户信息 服务实现类
 * </p>
 *
 * @author iinti
 * @since 2022-02-22
 */
@Service
class UserInfoService {

    @Resource
    private lateinit var userMapper: UserInfoMapper

    @Resource
    private lateinit var dbCacheManager: DbCacheManager

    companion object {
        private const val salt = BuildInfo.appName + "2023V2!@&*("

        fun byteToLong(b: ByteArray): Long {
            var s: Long
            val s0 = b[0].toInt() and 0xff // 最低位
            val s1 = b[1].toInt() and 0xff
            val s2 = b[2].toInt() and 0xff
            val s3 = b[3].toInt() and 0xff
            val s4 = b[4].toInt() and 0xff // 最低位
            val s5 = b[5].toInt() and 0xff
            val s6 = b[6].toInt() and 0xff
            val s7 = b[7].toInt() and 0xff

            // s0不变
            s = s0.toLong()
            s += (s1 shl 8).toLong()
            s += (s2 shl 16).toLong()
            s += (s3 shl 24).toLong()
            s += (s4 shl 8 * 4).toLong()
            s += (s5 shl 8 * 5).toLong()
            s += (s6 shl 8 * 6).toLong()
            s += (s7 shl 8 * 7).toLong()
            return s
        }

        fun longToByte(number: Long): ByteArray {
            var temp = number
            val b = ByteArray(8)
            for (i in b.indices) {
                b[i] = (temp and 0xff).toByte()
                // 将最低位保存在最低位
                temp = temp shr 8
                // 向右移8位
            }
            return b
        }

        // 用户名中非法的字符,这是因为我们的系统将会直接使用用户名称做业务，一些奇怪的字符串将会引起一些紊乱
        // 如鉴权表达式：传递控制信息
        //   资产文件：使用用户名隔离多个用户的文件存储
        val illegalUserNameChs = charArrayOf(' ', '-', '/', '\t', '\n', '*', '\\')
    }

    fun login(account: String, password: String): CommonRes<UserInfo> {
        val userInfo = userMapper
            .selectOne(QueryWrapper<UserInfo>().eq(UserInfo.USER_NAME, account).last("limit 1"))
        if (userInfo == null) {
            return CommonRes.failed("请检查用户名或密码")
        }
        if (!password.equals(userInfo.password)) {
            return CommonRes.failed("请检查用户名或密码")
        }

        val newToken = genLoginToken(userInfo, LocalDateTime.now())
        userInfo.loginToken = newToken
        userMapper.update(
            null, UpdateWrapper<UserInfo>().eq(UserInfo.USER_NAME, userInfo.userName)
                .set(UserInfo.LOGIN_TOKEN, newToken)
        )
        return CommonRes.success(userInfo)
    }

    fun resetUserPassword(userId: Long?, password: String): CommonRes<UserInfo> {
        if (password.isBlank()) {
            return CommonRes.failed("密码格式不正确。")
        }
        val userInfo = userMapper
            .selectOne(QueryWrapper<UserInfo>().eq(UserInfo.ID, userId).last("limit 1"))
        if (userInfo == null) {
            return CommonRes.failed("用户不存在")
        }
        userInfo.password = password
        userMapper.update(
            null, UpdateWrapper<UserInfo>().eq(UserInfo.USER_NAME, userInfo.userName)
                .set(UserInfo.PASSWORD, password)
        )
        BroadcastService.triggerEvent(BroadcastService.Topic.USER)
        return CommonRes.success(userInfo)
    }

    fun refreshToken(oldToken: String?): String? {
        val userInfo = getUserInfoFromToken(oldToken)
        if (userInfo == null) {
            //token不合法
            return null
        }
        if (isRightToken(oldToken!!, userInfo)) {
            return genLoginToken(userInfo, LocalDateTime.now())
        }

        return null
    }

    private fun isRightToken(token: String, userInfo: UserInfo): Boolean {
        for (i in 0..2) {

            // 每个token三分钟有效期，算法检测历史9分钟内的token，超过9分钟没有执行刷新操作，token失效
            val historyToken = genLoginToken(userInfo, LocalDateTime.now().minusMinutes(i * 3.toLong()))
            if (historyToken == token) {
                return true
            }
        }
        return false
    }

    fun genLoginToken(userInfo: UserInfo, date: LocalDateTime): String {
        val bytes = Md5Utils.md5Bytes(
            userInfo.userName + "|" + userInfo.password + "|" + salt + "|" +
                    (date.get(ChronoField.MINUTE_OF_DAY) / 30) + "|" + date.getDayOfYear()
        )
        //
        val userIdData = longToByte(userInfo.id!!)
        val finalData = ByteArray(bytes.size + userIdData.size)

        for (i in 0..7) {
            finalData[i * 2] = userIdData[i]
            finalData[i * 2 + 1] = bytes[i]
        }

        if (bytes.size - 8 >= 0) {
            System.arraycopy(bytes, 8, finalData, 16, bytes.size - 8)
        }

        return Md5Utils.toHexString(finalData)
    }

    fun register(account: String, password: String): CommonRes<UserInfo> {
        if (account.isNullOrBlank() || password.isNullOrBlank()) {
            return CommonRes.failed("用户或者密码不能为空")
        }
        if (illegalUserNameChs.any { ch -> account.contains(ch) }) {
            return CommonRes.failed("userName contain illegal character")
        }
        val userInfo = userMapper
            .selectOne(QueryWrapper<UserInfo>().eq(UserInfo.USER_NAME, account).last("limit 1"))

        if (userInfo != null) {
            return CommonRes.failed("用户已存在")
        }

        val adminCount = userMapper.selectCount(QueryWrapper<UserInfo>().eq(UserInfo.IS_ADMIN, true))

        val isCallFromAdmin = AppContext.getUser() != null && AppContext.getUser()!!.isAdmin!!
        if (!isCallFromAdmin && adminCount.toInt() != 0 && !Settings.allowRegisterUser.value!!) {
            return CommonRes.failed("当前系统不允许注册新用户，详情请联系管理员")
        }

        val newUser = UserInfo()
        newUser.userName = account
        newUser.password = password
        newUser.apiToken = UUID.randomUUID().toString()
        // 第一个注册用户，认为是管理员
        newUser.isAdmin = adminCount.toInt() == 0

        val result = userMapper.insert(newUser)
        if (result == 1) {
            val newToken = genLoginToken(newUser, LocalDateTime.now())
            newUser.loginToken = newToken
            userMapper.update(
                null, UpdateWrapper<UserInfo>().eq(UserInfo.USER_NAME, newUser.userName)
                    .set(UserInfo.LOGIN_TOKEN, newToken)
            )
            BroadcastService.triggerEvent(BroadcastService.Topic.USER)
            return CommonRes.success(newUser)
        }

        return CommonRes.failed("注册失败，请稍后再试")
    }

    fun checkAPIToken(tokenList: List<String>): CommonRes<UserInfo> {
        tokenList.forEach { token ->
            val userInfo = dbCacheManager.getUserCacheWithApiToken()?.getModeWithCache(token)
            if (userInfo != null) {
                return CommonRes.success(userInfo)
            }
        }

        val queryWrapper = QueryWrapper<UserInfo>().apply {
            if (tokenList.size == 1) {
                eq(UserInfo.API_TOKEN, tokenList[0])
            } else {
                `in`(UserInfo.API_TOKEN, tokenList)
            }
        }

        val userInfo = userMapper.selectOne(queryWrapper.last("limit 1"))
        if (userInfo != null) {
            return CommonRes.success(userInfo)
        }
        return CommonRes.failed("请登录")
    }

    fun checkLogin(tokenList: List<String>): CommonRes<UserInfo> {
        for (candidateToken in tokenList) {
            val res = checkLogin(candidateToken)
            if (res.isOk()) {
                return res
            }
        }
        return CommonRes.failed("请登录")
    }

    fun checkLogin(token: String): CommonRes<UserInfo> {
        val userInfo = getUserInfoFromToken(token)
        if (userInfo == null) {
            return CommonRes.failed(CommonRes.statusNeedLogin, "token错误")
        }
        if (!isRightToken(token, userInfo)) {
            return CommonRes.failed("请登录")
        }
        val newToken = genLoginToken(userInfo, LocalDateTime.now())
        userInfo.loginToken = newToken
        userMapper.update(
            null, UpdateWrapper<UserInfo>()
                .eq(UserInfo.USER_NAME, userInfo.userName)
                .set(UserInfo.LOGIN_TOKEN, newToken)
        )
        return CommonRes.success(userInfo)
    }

    private fun getUserInfoFromToken(token: String?): UserInfo? {
        // check token format
        if (token.isNullOrBlank()) {
            return null
        }
        if (token.length % 2 != 0) {
            //token长度必须是偶数
            return null
        }
        if (token.length < 16) {
            return null
        }
        for (ch in token.toCharArray()) {
            // [0-9] [a-f]
            if (ch in '0'..'9' || ch in 'a'..'f') {
                continue
            }
            //log.warn("broken token: {} reason:none dex character", token)
            return null
        }

        val bytes = Md5Utils.hexToByteArray(token)
        val longByteArray = ByteArray(8)
        // byte[] md5BeginByteArray = new byte[8];
        for (i in 0..7) {
            longByteArray[i] = bytes[i * 2]
            //  md5BeginByteArray[i] = bytes[i * 2 + 1];
        }
        val userId = byteToLong(longByteArray)
        val userInfo = dbCacheManager.getUserCacheWithId()!!.getModeWithCache(userId.toString())
        if (userInfo != null) {
            return userInfo
        }
        return userMapper.selectById(userId)
    }

    fun isUserExist(username: String): Boolean {
        return dbCacheManager.getUserCacheWithName()?.getModeWithCache(username) != null
    }

    fun grantAdmin(userName: String, isAdmin: Boolean): CommonRes<String> {
        val userInfo = userMapper.selectOne(QueryWrapper<UserInfo>().eq(UserInfo.USER_NAME, userName))
        if (userInfo == null) {
            return CommonRes.failed("user not exist")
        }
        if (userInfo.id!!.equals(AppContext.getUser()?.id)) {
            return CommonRes.failed("you can not operate yourself")
        }
        userInfo.isAdmin = isAdmin
        userMapper.update(
            null, UpdateWrapper<UserInfo>().eq(UserInfo.USER_NAME, userInfo.userName)
                .set(UserInfo.IS_ADMIN, isAdmin)
        )
        BroadcastService.triggerEvent(BroadcastService.Topic.USER)
        return CommonRes.success("ok")
    }

    fun editUserPerm(userName: String, permsConfig: String?): CommonRes<UserInfo> {
        return CommonRes.ofPresent(dbCacheManager.getUserCacheWithName()!!.getModeWithCache(userName))
            .ifOk { userInfo ->
                val map = PermsService.parseExp(permsConfig, false)

                val newConfig = PermsService.rebuildExp(map)
                userMapper.update(
                    null, UpdateWrapper<UserInfo>()
                        .eq(UserInfo.ID, userInfo.id)
                        .set(UserInfo.PERMISSION, newConfig)
                )
                BroadcastService.triggerEvent(BroadcastService.Topic.USER)
            }.transform { userInfo -> userMapper.selectById(userInfo.id) }

    }
}