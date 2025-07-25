package cn.iinti.atom.service.base;


import cn.iinti.atom.BuildInfo;
import cn.iinti.atom.entity.ServerNode;
import cn.iinti.atom.mapper.ServerNodeMapper;
import cn.iinti.atom.service.base.config.Configs;
import cn.iinti.atom.service.base.env.Environment;
import cn.iinti.atom.service.base.safethread.Looper;
import cn.iinti.atom.utils.IpUtil;
import cn.iinti.atom.utils.ServerIdentifier;
import cn.iinti.atom.utils.net.SimpleHttpInvoker;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import proguard.annotation.Keep;

import java.net.SocketException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class BroadcastService implements ApplicationListener<WebServerInitializedEvent> {

    private static final Looper sendThread = new Looper("broadcast-send").startLoop();
    private static final LinkedList<Topic> topicQueue = Lists.newLinkedList();

    private static BroadcastService instance;

    private Integer mPort;

    @Resource
    private ServerNodeMapper serverNodeMapper;

    private final Set<String> resolvedNodes = Sets.newConcurrentHashSet();

    private long lastForResolveLocal = 0;

    /**
     * 解析和写入心跳、服务IP、端口等服务器节点元数据，
     */
    @Scheduled(fixedRate = 60 * 1000)
    public void resolveAndHeartbeat() {
        BroadcastService.instance = this;
        if (Environment.isLocalDebug || mPort == null) {
            return;
        }

        String serverId = ServerIdentifier.id();
        ServerNode one = serverNodeMapper.selectOne(new QueryWrapper<ServerNode>()
                .eq(ServerNode.SERVER_ID, serverId));

        if (one == null) {
            one = new ServerNode();
            one.setServerId(serverId);
            one.setPort(mPort);
            one.setLastActiveTime(LocalDateTime.now());
            serverNodeMapper.insert(one);
            return;
        }

        one.setPort(mPort);
        one.setLastActiveTime(LocalDateTime.now());
        serverNodeMapper.updateById(one);

        boolean force = false;
        if (lastForResolveLocal < 1000 || System.currentTimeMillis() - lastForResolveLocal >
                5 * 60 * 60 * 1000) {
            lastForResolveLocal = System.currentTimeMillis();
            force = true;
        }

        resolveLocalNode(one, force);

        QueryWrapper<ServerNode> queryWrapper = new QueryWrapper<>();
        if (!resolvedNodes.isEmpty()) {
            queryWrapper.notIn(ServerNode.SERVER_ID, resolvedNodes);
        }
        queryWrapper.ne(ServerNode.SERVER_ID, serverId)
                .ge(ServerNode.LAST_ACTIVE_TIME, LocalDateTime.now().minusMinutes(5));
        serverNodeMapper.selectList(queryWrapper).forEach(this::resolveOtherNode);
    }


    private void resolveLocalNode(ServerNode serverNode, boolean force) {
        boolean update = false;
        if (force || StringUtils.isBlank(serverNode.getLocalIp())) {
            try {
                serverNode.setLocalIp(IpUtil.getLocalIps());
                if (StringUtils.isNotEmpty(serverNode.getLocalIp())) {
                    update = true;
                }
            } catch (SocketException e) {
                //ignore
            }
        }

        if (force || StringUtils.isBlank(serverNode.getOutIp())) {
            serverNode.setOutIp(IpUtil.getOutIp());
            if (StringUtils.isNotEmpty(serverNode.getOutIp())) {
                update = true;
            }
        }

        if (update) {
            serverNodeMapper.updateById(serverNode);
        }
    }

    private void resolveOtherNode(ServerNode serverNode) {
        if (serverNode.getPort() == null) {
            return;
        }

        List<String> testIp = Lists.newArrayListWithExpectedSize(3);
        if (StringUtils.isNotBlank(serverNode.getIp())) {
            testIp.add(serverNode.getIp());
        }
        if (StringUtils.isNotBlank(serverNode.getLocalIp())) {
            testIp.addAll(Arrays.asList(serverNode.getLocalIp().split(",")));
        }

        if (StringUtils.isNotBlank(serverNode.getOutIp())) {
            testIp.add(serverNode.getOutIp());
        }

        for (String task : testIp) {
            if (checkNodeServer(task, serverNode)) {
                serverNode = serverNodeMapper.selectById(serverNode.getId());
                serverNode.setIp(task);
                serverNodeMapper.updateById(serverNode);
                resolvedNodes.add(serverNode.getServerId());
                return;
            }
        }

        // 所有IP都无法解析，那么执行端口扫描？？？
    }

    private static boolean checkNodeServer(String ip, ServerNode serverNode) {
        String url = buildOtherNodeURL(ip, serverNode.getPort(), "exchangeClientId");
        String response = SimpleHttpInvoker.get(url);
        if (StringUtils.isBlank(response)) {
            log.info("exchangeClientId with ioException :{}", url, SimpleHttpInvoker.getIoException());
            return false;
        }
        JSONObject jsonObject;
        try {
            jsonObject = JSONObject.parseObject(response);
        } catch (JSONException e) {
            return false;
        }
        return serverNode.getServerId().equals(jsonObject.getString("data"));
    }

    private static final Multimap<Topic, IBroadcastListener> registry = HashMultimap.create();


    public static void register(Topic topic, IBroadcastListener listener) {
        registry.put(topic, listener);
    }

    public static String callListener(String topic) {
        if (StringUtils.isBlank(topic)) {
            return "false";
        }
        Topic value;
        try {
            value = Topic.valueOf(topic.toUpperCase(Locale.ROOT));
        } catch (NoSuchElementException noSuchElementException) {
            return "false";
        }
        BroadcastService.callListener(value);
        return "true";
    }

    /**
     * 调用当前进程内的监听器
     *
     * @param topic 主题
     */
    public static void callListener(Topic topic) {
        log.info("call broadcast with topic: {}", topic);
        Collection<IBroadcastListener> iBroadcastListeners = registry.get(topic);
        sendThread.post(() -> {
            for (IBroadcastListener listener : iBroadcastListeners) {
                try {
                    log.info("call listener: {}", listener);
                    listener.onBroadcastEvent();
                } catch (Throwable throwable) {
                    log.error("call broadcast listener failed!!", throwable);
                }
            }
        });
    }


    /**
     * 广播事件到所有机器节点
     *
     * @param topic 主题
     */
    public static void triggerEvent(Topic topic) {
        // 消重模型
        //   1. 单线程任务投递，任务为队列模型
        //   2. 单线程消费，并且确保投递任务之后一定会有一个消费action
        //   3. 消费从队列尾部获取任务，在获取一个待执行任务后，判定是否在队列中还有相同topic，如果有则证明存在重复的主题事件，
        //      此时由于在未来一定还会有refresh，故可以取消本次消息触发（原则为最后消息为最新消息，最新消息保证一定达到最终状态,同主题非最新action可以被过滤取消）
        sendThread.offerLast(() -> topicQueue.addFirst(topic));
        sendThread.offerLast(() -> {
            Topic last = topicQueue.removeLast();
            if (last == null) {
                return;
            }
            if (topicQueue.contains(last)) {
                return;
            }
            callListener(topic);
            List<ServerNode> ServerNodes = instance.serverNodeMapper
                    .selectList(new QueryWrapper<ServerNode>().eq(ServerNode.ENABLE, true));
            for (ServerNode node : ServerNodes) {
                if (ServerIdentifier.id().equals(node.getServerId())) {
                    //当前机器本身不用广播通知，直接调用即可
                    continue;
                }
                sendToOtherThread(topic, node);
            }
        });

    }

    private static void sendToOtherThread(Topic topic, ServerNode serverNode) {
        log.info("broadcast :{} to node:{}", topic, serverNode.getServerId());
        if (StringUtils.isBlank(serverNode.getIp())) {
            log.error("can not resolve server node ,skip broadcast message");
            return;
        }
        String url = buildOtherNodeURL(serverNode.getIp(), serverNode.getPort(), "triggerBroadcast?topic=" + topic);
        String response = SimpleHttpInvoker.get(url);
        if (StringUtils.isBlank(url)) {
            log.error("call remote node IO error", SimpleHttpInvoker.getIoException());
            return;
        }
        log.info("response: {}", response);
    }

    private static String buildOtherNodeURL(String ip, int port, String api) {
        String protocol = Configs.getConfig("website.protocol", "http");
        String ret = protocol + "://" + ip + ":" + port + BuildInfo.restfulApiPrefix + "/system";
        boolean skipSlash = api.startsWith("/");
        return ret + (skipSlash ? "" : "/") + api;
    }


    @Override
    public void onApplicationEvent(WebServerInitializedEvent webServerInitializedEvent) {
        mPort = webServerInitializedEvent.getWebServer().getPort();
        post(this::resolveAndHeartbeat);
    }


    public static void post(Runnable runnable) {
        sendThread.post(runnable);
    }


    /**
     * 广播实践监听器
     */
    public interface IBroadcastListener {
        void onBroadcastEvent();
    }

    @Keep
    public enum Topic {
        CONFIG, // 配置文件变更
        USER,// 用户数据变更，主要是注册，账号密码修改
        METRIC_TAG,// 监控指标
    }
}
