package cn.iinti.atom.service.base.dbcache.exs

import cn.iinti.atom.entity.UserInfo
import cn.iinti.atom.service.base.perm.PermsService

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