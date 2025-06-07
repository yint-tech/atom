package cn.iinti.atom.service.base.dbcache

import cn.iinti.atom.entity.UserInfo
import cn.iinti.atom.mapper.UserInfoMapper
import cn.iinti.atom.service.base.BroadcastService
import cn.iinti.atom.service.base.dbcache.exs.UserEx
import jakarta.annotation.PostConstruct
import jakarta.annotation.Resource
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class DbCacheManager {
    @Resource
    private lateinit var userInfoMapper: UserInfoMapper

    var userCacheWithName: DbCacheStorage<UserInfo, UserEx>? = null
    var userCacheWithId: DbCacheStorage<UserInfo, Void>? = null
    var userCacheWithApiToken: DbCacheStorage<UserInfo, Void>? = null


    @Scheduled(fixedDelay = 5 * 60 * 1000)
    private fun updateAllDbData() {
        BroadcastService.post {
            userCacheWithName!!.updateAll()
            userCacheWithApiToken!!.updateAll()
            userCacheWithId!!.updateAll()
        }
    }

    @PostConstruct
    fun init() {
        userCacheWithName = DbCacheStorage(UserInfo.USER_NAME, userInfoMapper, updateHandlerUser)
        userCacheWithApiToken = DbCacheStorage(UserInfo.API_TOKEN, userInfoMapper)
        userCacheWithId = DbCacheStorage(UserInfo.ID, userInfoMapper)
        BroadcastService.register(BroadcastService.Topic.USER) {
            userCacheWithName!!.updateAll()
            userCacheWithApiToken!!.updateAll()
            userCacheWithId!!.updateAll()
        }
    }

    private val updateHandlerUser: DbCacheStorage.UpdateHandler<UserInfo, UserEx> =
        object : DbCacheStorage.UpdateHandler<UserInfo, UserEx> {
            override fun doUpdate(m: UserInfo, e: UserEx?): UserEx {
                val ex = e ?: UserEx()
                ex.reload(m)
                return ex
            }
        }
}