# 构建和发布
atom是一个从软件产品层面考虑的项目，所以atom的构建以``zip``压缩包形式作为产出物。具体表现为：

- 脚本目录
  - 提供window、Linux平台的启动脚本
  - 自动升级软件脚本（从因体官方存储下载更新包）
  - 重启和守护
- 配置目录：所有可以提供给用户直接修改，或者定制参数的部分
  - 数据库，项目定制化配置等
  - 文档页面内容
  - 网页静态资源
  - 内置资产下载：如前端项目工程、文档工程，其他需要给用户提供下载的内置资源等
- 资产目录：atom发布后，给用户提供部署相关支持资源
  - 数据库建表脚本
  - 其他
- 日志目录：软件运行时日志出现在本目录
- lib：所有java依赖库，atom业务代码编译结果
- storage：存储目录，如果atom使用了某些文件，内嵌数据库，索引等资产

可以看到，和微服务模式相比，作为给客户使用的外部产品，atom提供了更多的修改范围。所以atom的构建和微服务单一文件发布产品项目，会不太一样。

# 发布
Atom支持在从代码编译的、代码上传、代码发布全部流程。他特别适合个人开发者，因为整个发布过程不依赖任何三方发布系统。

## 预先条件
在发布之前，你需要在服务器提前准备好两部分
- jdk： 在服务器安装jdk17以上的版本
- 创建或者购买一个mysql数据库，并且使用建表语句``server/src/main/resources/develop-atom/ddl.sql``完成数据库的初始化
- 记录mysql的基本信息，供后续发布配置使用：数据库地址、数据库账户、数据库密码、数据库名

## 配置
在发布之前，您需要配置发布的服务器，数据库配置等参数。好在atom对系统配置做了最简化控制，基本上你只需要配置很少的内容即可

### profile介绍
atom支持多组发布配置profile，和普通SpringBoot项目不同的是，atom将前端、后端、文档、发布服务器信息等信息打包作为一个profile。
这样一个profile可以描述一组发布目标，并且同时具有完整的前后端、服务器列表等内容。

profile满足如下约定规则

- profile按照properties文件放置在``deploy``文件夹下
- profile文件要求以``app_``开始，以``.properties``结尾，如``app_A业务.properties``,``app_测试网站.properties``
- 具备相似配置的发布profile可以放到同一个文件内，同时``deploy``文件夹下可以放置多个profile文件，每个文件互不干扰

### Gradle支持
Atom的gradle脚本将会扫描``profile``目录下的配置文件，并且为每个文件构建发布任务。其基本规则为提取``app_${id}.properties]``的${id}部分，
并为他注册一个发布名为:``deploy-${id}``的gradleTask

- 如：``app_demoSite.properties``可以使用命令行：``./gradlew deploy-demoSite``进行发布
- 如：``app_A业务.properties``可以使用命令行：``./gradlew deploy-A业务``进行发布

同时如果你使用idea，或者androidStudio开发，则可以在右侧Gradle工具栏直接双击对应task完成发布。如下图：

**TODO 图片**

### profile内容说明
如下为最简配置，你只需要配置服务端口、数据库链接信息、发布的ip地址即可
```properties
server.port=8951
spring.datasource.username=iinti_test
spring.datasource.password=iinti^2024
spring.datasource.url=jdbc:mysql://10.42.2.1:30180/atom_demo?useSSL=true&useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&serverTimezone=Asia/Shanghai
deploy.host=atom.iinti.cn
```
可以想到，对于一个完整的项目来说，如上配置有很多内容是没有描述的。这是因为还有部分配置项有默认值，当你有定制化调整的需求是，可以按需调整

```properties
# 完整配置
server.port=8951
spring.datasource.username=iinti_test
spring.datasource.password=iinti^2024
spring.datasource.url=jdbc:mysql://10.42.2.1:30180/atom_demo?useSSL=true&useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&serverTimezone=Asia/Shanghai

# host可以指定多个，以逗号分割，这样可以支持同时对多台集群进行发布
deploy.host=atom.iinti.cn

# 开启demoSite开关，当demoSite开关被开启后，atom部分网将会处于《只读》模式，比如不允许修改系统设置，他特别适合作为一个演示网站使用
env.demoSite=true

# 发布过程其实需要通过ssh登入入服务器，本配置为登录用户名，默认为：root，如果你的服务器登录账户不是root，则配置为正确的用户名即可
deploy.user=

# 服务器的登录密码，此配置可以为空，当密码为空是，atom发布将会通过免密码的方式登录i下哦给你个
deploy.password=
# 如果你没有配置密码则此配置生效，此配置为通过公钥免登录的方式登入服务器，此时需要指定私钥，默认为ssh的私钥存储文件：~/.ssh/id_rsa
deploy.identity=

# 系统发布的目标文件夹，默认为: /opt/atom/
deploy.workdir=

# 跳板机，如果你的发布服务器是一个内网服务器，需要通过跳板机的方式登录，则下列配置为跳板机链接方式
# 他和普通的deploy.xxx含义完全一致
deploy.jump.user=
deploy.jump.host=
deploy.jump.password=
deploy.jump.identity=
```
