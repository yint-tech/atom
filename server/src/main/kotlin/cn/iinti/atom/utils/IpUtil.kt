package cn.iinti.atom.utils

import cn.iinti.atom.service.base.config.Settings
import cn.iinti.atom.utils.net.SimpleHttpInvoker
import com.google.common.collect.Maps
import com.google.common.collect.Sets
import org.apache.commons.lang3.StringUtils
import java.net.*
import java.util.*

import java.util.regex.Pattern


object IpUtil {
    private val ipPattern = Pattern.compile("""\d+.\d+.\d+.\d+""")

    /**
     * 解析本机IP
     */
    @Throws(SocketException::class)

    fun getLocalIps(): String {
        var ips: MutableSet<String> = Sets.newHashSet()
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            if (networkInterface.isLoopback || networkInterface.isVirtual || networkInterface.isPointToPoint) {
                continue
            }
            if (!networkInterface.isUp) {
                continue
            }
            val inetAddresses = networkInterface.inetAddresses
            while (inetAddresses.hasMoreElements()) {
                val inetAddress = inetAddresses.nextElement()
                if (inetAddress is Inet4Address && !inetAddress.isLoopbackAddress()) {
                    ips.add(inetAddress.getHostAddress())
                }
            }
        }


        try {
            DatagramSocket().use { datagramSocket ->
                datagramSocket.connect(InetAddress.getByName("8.8.8.8"), 10002)
                val localAddress = datagramSocket.localAddress
                if (localAddress is Inet4Address) {
                    ips.add(localAddress.getHostAddress())
                }
            }
        } catch (ignore: UnknownHostException) {
        }


        if (ips.size > 4) {
            // 有docker混部的设备，内网地址特别多,这个时候以A网网段划分，每个A网段取一个
            // 否则数据库字段长度不够，存不下，正常的极其理论上内网地址是不超过两个的才对
            val filterMap: MutableMap<String, String> = Maps.newHashMap()
            ips.forEach { s: String ->
                val dotIndex = s.indexOf(".")
                val aSegment = s.substring(0, dotIndex)
                if (aSegment == "192" || aSegment == "10" || aSegment == "172") {
                    // 内网地址
                    filterMap[aSegment] = s
                } else {
                    // 外网地址则不进行切割，因为外网地址是重要的通信探测通道
                    filterMap[s] = s
                }
            }
            ips = HashSet(filterMap.values)
        }

        return StringUtils.join(ips, ",")
    }

    fun getOutIp(): String? {
        val outIp: String = SimpleHttpInvoker.get(Settings.outIpTestUrl.value!!)
        if (isValidIp(outIp)) {
            return outIp
        }
        val outIp1 = "https://myip.ipip.net/"
        if (outIp1.isBlank()) {
            return null
        }
        val matcher = ipPattern.matcher(outIp1)
        return if (matcher.find()) {
            val group = matcher.group()
            if (isValidIp(group)) {
                group
            } else {
                null
            }
        } else {
            null
        }
    }

    fun isValidIp(ip: String?): Boolean {
        if (ip.isNullOrBlank()) {
            return false
        }
        val segments = ip.split(".".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (segments.size != 4) {
            return false
        }
        for (segment in segments) {
            val i = segment.toIntOrNull() ?: -1
            if (i < 0 || i > 255) {
                return false
            }
        }
        return true
    }
}