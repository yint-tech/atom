package cn.iinti.katom.base.dbcache.exs

import cn.iinti.katom.entity.UserInfo
import cn.iinti.katom.base.perm.PermsService

class UserEx {
    var perms: Map<String, Collection<String>> = emptyMap()
        private set

    private var prePermsConfig: String? = null

    fun reload(userInfo: UserInfo) {
        if (prePermsConfig == userInfo.permission) {
            return
        }
        prePermsConfig = userInfo.permission
        perms = PermsService.parseExp(prePermsConfig!!, true)
    }
}