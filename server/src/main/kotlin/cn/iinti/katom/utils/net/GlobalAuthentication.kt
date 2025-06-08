package cn.iinti.katom.utils.net

import java.net.Authenticator
import java.net.PasswordAuthentication

object GlobalAuthentication {
    // 账号代理的账号密码
    private val threadLocalProxyAuth = ThreadLocal<AuthHolder>()

    fun setupAuthenticator() {
        Authenticator.setDefault(object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                val authHolder = threadLocalProxyAuth.get()
                return if (authHolder == null) {
                    super.getPasswordAuthentication()
                } else {
                    PasswordAuthentication(authHolder.user, authHolder.pass)
                }
            }
        })
    }

    fun setProxyAuth(user: String, pass: String) {
        threadLocalProxyAuth.set(AuthHolder(user, pass))
    }
}