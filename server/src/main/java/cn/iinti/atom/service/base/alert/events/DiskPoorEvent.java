package cn.iinti.atom.service.base.alert.events;

public record DiskPoorEvent(
        long totalSpace,
        long freeSpace,
        String serverId
) {
}
