package cn.iinti.atom.service.base.alert.events

data class DiskPoorEvent(
    val totalSpace: Long,
    val freeSpace: Long,
    val serverId: String
)