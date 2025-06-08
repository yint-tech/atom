package cn.iinti.katom.base.alert.events

data class DiskPoorEvent(
    val totalSpace: Long,
    val freeSpace: Long,
    val serverId: String
)