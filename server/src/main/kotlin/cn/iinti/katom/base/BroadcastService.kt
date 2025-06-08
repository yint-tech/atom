package cn.iinti.katom.base

import cn.iinti.atom.BuildInfo
import cn.iinti.katom.entity.ServerNode
import cn.iinti.katom.mapper.ServerNodeMapper
import cn.iinti.katom.base.config.Configs
import cn.iinti.katom.base.env.Environment
import cn.iinti.katom.base.safethread.Looper
import cn.iinti.katom.utils.IpUtil
import cn.iinti.katom.utils.ServerIdentifier
import cn.iinti.katom.utils.net.SimpleHttpInvoker
import com.alibaba.fastjson.JSONObject
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.google.common.collect.HashMultimap
import com.google.common.collect.Lists
import com.google.common.collect.Multimap
import com.google.common.collect.Sets
import jakarta.annotation.Resource
import org.slf4j.LoggerFactory
import org.springframework.boot.web.context.WebServerInitializedEvent
import org.springframework.context.ApplicationListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import proguard.annotation.Keep
import java.net.SocketException
import java.time.LocalDateTime
import java.util.*

// 添加StringUtils的import
import org.apache.commons.lang3.StringUtils

@Service
class BroadcastService : ApplicationListener<WebServerInitializedEvent> {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private val sendThread = Looper("broadcast-send").startLoop()
        private val topicQueue = Lists.newLinkedList<Topic>()
        private lateinit var instance: BroadcastService

        // 添加mPort字段
        private var mPort: Int? = null

        private val registry: Multimap<Topic, IBroadcastListener> = HashMultimap.create()

        fun register(topic: Topic, listener: IBroadcastListener) {
            registry.put(topic, listener)
        }

        fun callListener(topic: String): String {
            if (topic.isBlank()) {
                return "false"
            }
            val value: Topic = try {
                Topic.valueOf(topic.toUpperCase(Locale.ROOT))
            } catch (noSuchElementException: NoSuchElementException) {
                return "false"
            }
            callListener(value)
            return "true"
        }

        private fun callListener(topic: Topic) {
            val logger = LoggerFactory.getLogger(BroadcastService::class.java)
            logger.info("call broadcast with topic: $topic")
            val iBroadcastListeners = registry[topic]
            sendThread.post {
                for (listener in iBroadcastListeners) {
                    try {
                        logger.info("call listener: $listener")
                        listener.onBroadcastEvent()
                    } catch (throwable: Throwable) {
                        logger.error("call broadcast listener failed!!", throwable)
                    }
                }
            }
        }

        fun triggerEvent(topic: Topic) {
            // 消重模型
            //   1. 单线程任务投递，任务为队列模型
            //   2. 单线程消费，并且确保投销任务之后一定会有一个消费action
            //   3. 消费从队列尾部获取任务，在获取一个待执行任务后，判定是否在队列中还有相同topic，如果有则证明存在重复的主题事件，
            //      此时由于在未来一定还会有refresh，故可以取消本次消息触发（原则为最后消息为最新消息，最新消息保证一定达到最终状态,同主题非最新action可以被过滤取消）
            sendThread.offerLast { topicQueue.addFirst(topic) }
            sendThread.offerLast {
                val last = topicQueue.removeLast() ?: return@offerLast
                if (topicQueue.contains(last)) {
                    return@offerLast
                }
                callListener(topic)
                val serverNodes = instance.serverNodeMapper
                    .selectList(QueryWrapper<ServerNode>().eq(ServerNode.ENABLE, true))
                for (node in serverNodes) {
                    if (ServerIdentifier.id() == node.serverId) {
                        //当前机器本身不用广播通知，直接调用即可
                        continue
                    }
                    sendToOtherThread(topic, node)
                }
            }
        }

        private fun sendToOtherThread(topic: Topic, serverNode: ServerNode) {
            val logger = LoggerFactory.getLogger(BroadcastService::class.java)
            logger.info("broadcast :$topic to node:${serverNode.serverId}")
            if (StringUtils.isBlank(serverNode.ip)) {
                logger.error("can not resolve server node ,skip broadcast message")
                return
            }
            val url = buildOtherNodeURL(serverNode.ip!!, serverNode.port!!, "triggerBroadcast?topic=$topic")
            val response = SimpleHttpInvoker.get(url)
            if (StringUtils.isBlank(url)) {
                logger.error("call remote node IO error", SimpleHttpInvoker.getIoException())
                return
            }
            logger.info("response: $response")
        }

        private fun buildOtherNodeURL(ip: String, port: Int, api: String): String {
            val protocol = Configs.getConfig("website.protocol", "http")
            val ret = "$protocol://$ip:$port${BuildInfo.restfulApiPrefix}/system"
            val skipSlash = api.startsWith("/")
            return ret + (if (skipSlash) "" else "/") + api
        }

        fun post(runnable: () -> Unit) {
            sendThread.post(runnable)
        }
    }

    @Resource
    private lateinit var serverNodeMapper: ServerNodeMapper

    private val resolvedNodes = Sets.newConcurrentHashSet<String>()

    private var lastForResolveLocal = 0L

    @Scheduled(fixedRate = 60 * 1000)
    fun resolveAndHeartbeat() {
        instance = this
        if (Environment.isLocalDebug || mPort == null) {
            return
        }

        val serverId = ServerIdentifier.id()
        var one = serverNodeMapper.selectOne(
            QueryWrapper<ServerNode>()
                .eq(ServerNode.SERVER_ID, serverId)
        )

        if (one == null) {
            one = ServerNode()
            one.serverId = serverId
            one.port = mPort
            one.lastActiveTime = LocalDateTime.now()
            serverNodeMapper.insert(one)
            return
        }

        one.port = mPort
        one.lastActiveTime = LocalDateTime.now()
        serverNodeMapper.updateById(one)

        var force = false
        if (lastForResolveLocal < 1000 || System.currentTimeMillis() - lastForResolveLocal > 5 * 60 * 60 * 1000) {
            lastForResolveLocal = System.currentTimeMillis()
            force = true
        }

        resolveLocalNode(one, force)

        val queryWrapper = QueryWrapper<ServerNode>()
        if (resolvedNodes.isNotEmpty()) {
            queryWrapper.notIn(ServerNode.SERVER_ID, resolvedNodes)
        }
        queryWrapper.ne(ServerNode.SERVER_ID, serverId)
            .ge(ServerNode.LAST_ACTIVE_TIME, LocalDateTime.now().minusMinutes(5))
        serverNodeMapper.selectList(queryWrapper).forEach(this::resolveOtherNode)
    }

    private fun resolveLocalNode(serverNode: ServerNode, force: Boolean) {
        var update = false
        if (force || StringUtils.isBlank(serverNode.localIp)) {
            try {
                serverNode.localIp = IpUtil.getLocalIps()
                if (StringUtils.isNotEmpty(serverNode.localIp)) {
                    update = true
                }
            } catch (e: SocketException) {
                //ignore
            }
        }

        if (force || StringUtils.isBlank(serverNode.outIp)) {
            serverNode.outIp = IpUtil.getOutIp()
            if (StringUtils.isNotEmpty(serverNode.outIp)) {
                update = true
            }
        }

        if (update) {
            serverNodeMapper.updateById(serverNode)
        }
    }

    private fun resolveOtherNode(serverNode: ServerNode) {
        if (serverNode.port == null) {
            return
        }

        val testIp: MutableList<String> = Lists.newArrayListWithExpectedSize(3)
        if (StringUtils.isNotBlank(serverNode.ip)) {
            testIp.add(serverNode.ip!!)
        }
        if (StringUtils.isNotBlank(serverNode.localIp)) {
            testIp.addAll(serverNode.localIp!!.split(",").toList())
        }

        if (StringUtils.isNotBlank(serverNode.outIp)) {
            testIp.add(serverNode.outIp!!)
        }

        for (task in testIp) {
            if (checkNodeServer(task, serverNode)) {
                val serverNode = serverNodeMapper.selectById(serverNode.id)
                serverNode.ip = task
                serverNodeMapper.updateById(serverNode)
                resolvedNodes.add(serverNode.serverId)
                return
            }
        }

        // 所有IP都无法解析，那么执行端口扫描？？？
    }

    private fun checkNodeServer(ip: String, serverNode: ServerNode): Boolean {
        val url = buildOtherNodeURL(ip, serverNode.port!!, "exchangeClientId")
        val response = SimpleHttpInvoker.get(url)
        if (StringUtils.isBlank(response)) {
            log.info("exchangeClientId with ioException :$url", SimpleHttpInvoker.getIoException())
            return false
        }
        val jsonObject: JSONObject = try {
            JSONObject.parseObject(response)
        } catch (e: Exception) {
            return false
        }
        return serverNode.serverId.equals(jsonObject.getString("data"))
    }

    override fun onApplicationEvent(webServerInitializedEvent: WebServerInitializedEvent) {
        mPort = webServerInitializedEvent.webServer.port
        post(this::resolveAndHeartbeat)
    }

    /**
     * 广播实践监听器
     */
    fun interface IBroadcastListener {
        fun onBroadcastEvent()
    }

    @Keep
    enum class Topic {
        CONFIG, // 配置文件变更
        USER, // 用户数据变更，主要是注册，账号密码修改
        METRIC_TAG; // 监控指标
    }
}