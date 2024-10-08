# 配置中心
对于大多数企业中间件，配置中心基本都是核心组件之一。配置中心提供对设置功能的统一管理。对于配置中心对于在线系统的作用，可以参考这篇文章[微服务架构之「 配置中心 」](https://zhuanlan.zhihu.com/p/66097586)

## Atom配置中心

由于Atom定位为单体应用，所以其实没有**中心**的概念，Atom需要实现服务部署独立，即不能产生对外部系统的依赖。所以配置模块来说，Atom将配置存储在本系统的数据库中，
Atom有一张表用于存储所有的在线配置。再次基础上，Config模块提供如下预设功能封装。

- 基于静态工具函数的读写API：Atom保证系统启动后第一件事便加载数据库的配置，这样所有业务模块可以通过全局静态API访问配置中心
- 提供和静态配置的merge功能：即融合静态文件配置和数据库的在线配置，实现配置内容兜底
- 提供配置内容格式校验API：此功能非常重要，他可以保证用户提交的配置内容是符合格式要求的，这避免错误的配置内容导致业务功能错误（大部分配置中心没有这个功能，在一般的实践中都是多人check来确保配置正确）
- 提供配置到model的自动转换：
  - 业务拿到的配置内容可以是解析后的模型，而不是原始的配置字符串
  - 提供配置模型解析换器，当解析过程比较复杂的时候，模型缓存可以保证只有相关配置发生变更之后才会重新触发配置内容解析
  - 提供基本类型的预设转换器：字符串、数字、布尔等常见类型
- 提供配置变更监听器，用于业务主动感知配置变化，实现更加灵活的定制策略
- 提供前端页面
  - 配置服务-数据库-业务API链路闭环，即业务声明一个配置说明，便会在前端页面渲染其设置设置项
  - 提供配置项默认兜底策略，这样前端没有配置策略时，业务可以使用默认配置

## API说明
Atom的配置，仅支持key-value模型，但是value可以支持大文本。由于Atom是单体应用，所以不会存在多个配置环境、多个配置文件的问题。

### 读取
如下，获取一个key:``website.protocol``的设置，并且使用默认值：``http``进行兜底，配置模块默认类型为字符串
```java
String protocol = Configs.getConfig("website.protocol", "http");
```

对于其他类型配置，入boolean，int，提供相对于的API

- Int：Configs.getInt
- Boolean: Configs.getBoolean
- Double: Configs.getDouble

### 监听器

任何配置发生变更
```
Configs.addConfigChangeListener(() -> {
    System.out.println("配置发生变更");
});
```

某个监控固定key的变更事件
```
Configs.addKeyMonitor("xxxKey",() -> {
    System.out.println("配置xxxKey发生变更");
});
```

## 模型转换
Configs提供一个模型抽象，``ConfigValue<V>``,其中``V``便是抽象模型类型，使用``ConfigValue<V>``创建的变量，将会获得自动监听配置key和转换到对应模型的能力，
当然，为了达成目标，你需要提供如下两个要素：

- 对应的配置key，用以实现对目标配置的监听，此配置在抽象的构造函数被要求提供
- 对应的转换规则，即如何将配置内容转换为模型
  - 你可以在继承``ConfigValue<V>``后，通过重写方法提供此转换器
  - 如果转换模型是一个简单的json，或者java基础类型，则框架已经完成默认的转换适配，即可以默认传递null，由框架自行识别

同时，Configs提供如下默认基础类型：

- StringConfigValue：字符串
- MultiLineStrConfigValue：多行字符串，和普通字符串不一样的是，在前端页面，他会渲染为多行文本，用于支持json、脚本等需要多行配置的场景。但是在后端规则上，他和StringConfigValue行为一致
- BooleanConfigValue：boolean类型，多用于在线功能开关
- IntegerConfigValue： 数字类型，多用于在线量级开关。如线程数量、并发数量等

### Settings
在模型转换之上，为了在业务上进一步实现对配置的分组和管理，我专门抽离了一个给业务配置的文件，用于专门维护和管理配置描述。所以他是对于``ConfigValue<V>``的业务使用列表。

在Settings中，用户可以声明配置项，然后在业务侧对配置内容进行只用。

#### 声明配置

```java
    public static final Configs.BooleanConfigValue allowRegisterUser = newBooleanConfig(
            BuildInfo.appName + ".user.allowRegister", false, "是否允许注册用户",
            "设置不允许注册新用户，则可以避免用户空白注册，规避系统安全机制不完善，让敏感数据通过注册泄漏"
    );
```

#### 使用配置
```
        boolean isCallFromAdmin = AppContext.getUser() != null && AppContext.getUser().getIsAdmin();
        if (!isCallFromAdmin && adminCount != 0 && !Settings.allowRegisterUser.value) {
            return CommonRes.failed("当前系统不允许注册新用户，详情请联系管理员");
        }
```

#### 自定义模型转换
如果你的配置是一个解析成本很高的项目（如脚本编译），并且内置的基础类型没有覆盖。则可能需要考虑自定义模型转换器。此时最佳的方法就是调用``newCustomConfig``方法，
在内置模板中，存在一个用于报警的扩展脚本模块，他的配置是一段groovy脚本代码，你可以参考他的声明方法
```java
    public static Configs.ConfigValue<EventScript> eventNotifyScript = newCustomConfig(CustomConfigBuilder.<EventScript>builder()
            .configKey(BuildInfo.appName + ".eventNotifyScript")
            .transformFunc((value, type) -> {
                if (StringUtils.isNotBlank(value)) {
                    // 在这里调用脚本编译器将文本编译为一个java对象，系统保证只有配置改变了才会触发此编译刷新流程
                    return ScriptFactory.compileScript(value, EventScript.class);
                }
                return null;

            })
            .desc("事件通知处理脚本")
            .detailDesc("内部事件，通过调用脚本的方式通知到外部系统")
            .configType("multiLine")
            .build()
    );
```

同样，在业务侧可以通过很简单的索引访问到编译好的模型：
```java
    private void withScriptExtension(Consumer<EventScript> fun) {
        // 这里访问了在上一步创建的eventNotifyScript配置变量，他的value永远是最新的一次配置转换而来
        EventScript eventScript = Settings.eventNotifyScript.value;
        if (eventScript == null) {
            return;
        }
        eventNotifierThread.post(() -> fun.accept(eventScript));
    }
```

## 配置格式检查
对于``ConfigValue<V>``，格式检查器将会自动调用检查（即调用``ConfigValue<V>``配置的模型转换器，观察是否会转换成功），
对于底层其他的配置，或者在``ConfigValue<V>``之外还有额外检查规则的情况， 请在``SettingsValidate``配置定制的格式检查规则

**请注意，在绝大数情况下，更加建议使用Settings的模型配置机制，他的使用更加贴合上层业务**

如对于URL的配置，他使用的是字符串格式，通过如下规则检查URL是否合法：
```java
    private static class URLValidator implements Validator {

        @Override
        public String doValidate(String value) {
            if (StringUtils.isBlank(value)) {
                return null;
            }
            try {
                new URL(value);
                return null;
            } catch (MalformedURLException e) {
                return e.getMessage();
            }
        }
    }
```