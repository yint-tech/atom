package cn.iinti.katom.utils.net

class AuthHolder(user: String, pass: String) {
    val user: String = user
    val pass: CharArray = pass.toCharArray()
}