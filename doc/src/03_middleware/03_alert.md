# 告警
在监控之后，另一个重要的功能就是告警了。通过告警机制，可以在系统发生某些意外事项的时候将消息发送出来，发送通道可以为电话、内部IM消息、邮箱、钉钉等外部聊天群组。
这样当系统出现故障时可以立即进行干预。

另外大部分情况下，告警时对监控指标的直接运用，因为监控指标本身就是反应系统各项运行数据。atom的告警机制也满足此规则。

## 告警脚本
在atom中，告警事件可能发生，但是和其他报警平台一样，告警模块在默认情况下并不知道将告警信息发送到何处。所以Atom在告警消息订阅和发出这里是高度抽象的，
用户需要实现告警脚本监听告警消息，并且确认如何处理告警消息。
> 告警通道一定需要依赖外部消息通道，如通过短信发送告警消息到某个手机号下，此时我们不知道手机号是啥，也没有短信通道API
> > 消息通道都是收费的

Atom的告警脚本是一段groovy脚本扩展，你可以在``系统设置``->``事件通知处理脚本``这里配置脚本内容，groovy脚本是一种高度灵活的语言，考虑用户对groovy可能不是特别熟悉，
我提供了一个相关语法检查和代码提示，只需要用idea编辑文件``server/src/test/groovy/AlertTest.groovy``即可
> idea是java最常用的代码编写IDE

- 如果您熟悉android开发，则android的gradle脚本在早期很大一段事件都是使用groovy语言（大家熟悉的build.gradle文件）
- 类似android开发，有相当一部分同学在很长一段时间知道如何编辑android构建脚本，但是并不会groovy语言。Atom告警脚本同样可以模仿demo写就行，不必强求学会语法
- 同时，groovy基本完全支持java语法，大多数情况下，直接把脚本当作java看来也是可以的

在demo脚本中，我们模拟了一种通过钉钉机器人发送报警消息的demo，提供了敏感接口访问、磁盘使用率-内置事件、磁盘使用率-监控指标三个demo
```groovy
import cn.iinti.atom.service.base.alert.EventScript
import groovy.json.JsonBuilder
import groovy.transform.BaseScript

// 此文件用于对实现自定义的系统webhook
// 用户可以通过配置扩展脚本的方式实现系统内部变脸和外部逻辑的交互
// 一个常用的场景是通过本脚本实现一些报警功能

@BaseScript EventScript _base
def DD_WEBHOOK = "https://oapi.dingtalk.com/robot/send?access_token=xxxxToken"

onSensitiveOperation {
    def json = new JsonBuilder()
    json {
        msgtype "text"
        text {
            content "【敏感接口访问】${message}"
        }
    }
    // 用户的敏感接口操作，通过webhook发送到钉钉机器人
    httpPost DD_WEBHOOK, json
}

onDiskPoor {
    def json = new JsonBuilder()
    json {
        msgtype "text"
        text {
            content "【磁盘不足】总大小： ${totalSpace()} 当前可用:${freeSpace()}"
        }
    }
    // 用户的敏感接口操作，通过webhook发送到钉钉机器人
    httpPost DD_WEBHOOK, json
}

// 监听metric，使用mql语法，是一种通用的感知系统内部指标进行报警的机制
mqlSubscribe {
    id "磁盘用量报警"
    mql """
可用空间 = metric(disk.free);
总空间 = metric(disk.total);

可用空间 = aggregate(可用空间,'serverId','path'); 
总空间 = aggregate(总空间,'serverId','path')

磁盘使用率 = ((总空间 - 可用空间) * 100) / 总空间;

可用空间 = 可用空间/1048576; 
总空间 = 总空间/1048576; 

show(可用空间,总空间,磁盘使用率);
"""
    cron "0 9 * * * ?"
    onMetric {
        if (getMetricValue("磁盘使用率") < 24) {
            def json = new JsonBuilder()
            json {
                msgtype "text"
                text {
                    content "【磁盘不足】总大小： ${getMetricValue("总空间")} 当前可用:${getMetricValue("可用空间")}"
                }
            }
            // 用户的敏感接口操作，通过webhook发送到钉钉机器人
            httpPost DD_WEBHOOK, json
            return
        }
    }
}
```

## 基于监控指标的告警
Atom内置通过定时任务执行mql脚本，获取当前指标状态的方式通知报警脚本，用户通过报警脚本对指标的监听注册，判断当前指标是否触达报警阈值。

> 再次之前，您需要了解上一个章节讲述的mql语言

```groovy
mqlSubscribe {
    // 给当前报警设定一个唯一id
    id "磁盘用量报警"
    // 通过mql获取和加工指标
    mql """
可用空间 = metric(disk.free);
show(可用空间);
"""
    // 通过cron表达式，指定报警指标触发的事件规则（如有些报警可能晚上并不需要感知）
    cron "0 9 * * * ?"
    onMetric {
        // 这里就是指标的计算回调，在这里可以拿到当前的指标情况（通过调用getMetricValue api）
        if (getMetricValue("磁盘使用率") < 24) {
            // 用户对指标的进行判断，加工为报警消息
            def json = new JsonBuilder()
            json {
                
            }
            // 用户自定义将消息通过http发送到消息通道：如钉钉机器人、微信机器人、短信通道等
            httpPost DD_WEBHOOK, json
            return
        }
    }
}
```

## 自定义告警事件
除开基于指标的告警，我们也可能存在当某些是发生了便立即发送webhook的需求。

- 如某些业务接口，当用户发生了调用，便会立即通知外部，如用用户充值，则代表有订单到来，需要通知业务操作员立即介入
- 如系统提供战报的功能，每当成单，就立刻发送战报通知。

在Atom内置的敏感接口告警通知功能就提供了告警消息之外的事件类型,他的功能是标注了alert的Restful接口，一旦用户进行了调用，便会立刻产生敏感接口访问事件。
我们可以通过脚本监听此事件，进行相关业务处理。

```groovy
onSensitiveOperation {
    def json = new JsonBuilder()
    json {
        msgtype "text"
        text {
            content "【敏感接口访问】${message}"
        }
    }
    // 用户的敏感接口操作，通过webhook发送到钉钉机器人
    httpPost DD_WEBHOOK, json
}
```
### 自定义事件开发
类似敏感事件接口访问通知，我们在``cn.iinti.atom.service.base.alert.EventScript.onSensitiveOperation``增加相关的方法，用用于接受脚本回调函数注册。

在您的事件发生时，参考``cn.iinti.atom.service.base.alert.EventNotifierService``，将事件构造为对象，调用上一步注册的回调函数即可
```java
    public void notifySensitiveOperation(String user, String api, String params) {
        withScriptExtension((eventScript -> {
            SensitiveOperationEvent event = new SensitiveOperationEvent(user, api, params);
            callExtensions(eventScript.sensitiveEventList, event);
        }));
    }

```

## 广告
Atom多用于小型系统开发，一般不具备完整的消息通知通道。在我们的实践中，大多数情况我们直接通过微信机器人作为消息通道，
具体原理为破解了微信的消息收发接口，控制微信号加入某个业务对接的微信群聊，并且使用正常的用户微信号发送消息，用于实现报警消息通知。

然而微信消息接口收发接口破解并不是一个合规的操作，所以我们不会对这部分代码进行开源。如果您的业务场景没有合规问题，也希望使用微信机器人作为告警通道，
可以联系我们商务合作对接
