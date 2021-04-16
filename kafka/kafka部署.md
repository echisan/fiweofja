# kafka部署方案

## 磁盘

规划磁盘容量考虑的因素，一般情况下 Kafka 集群除了消息数据还有其他类型的数据，比如索引数据等，需要再为这些数据预留出 10% 的磁盘空间

- 新增消息数
- 消息留存时间
- 平均消息大小
- 备份数
- 是否启用压缩

## 带宽

> 根据实际使用经验，超过 70% 的阈值就有网络丢包的可能性了，故 70% 的设定是一个比
>
> 较合理的值，也就是说单台 Kafka 服务器最多也就能使用大约 700Mb 的带宽资源。
>
> 稍等，这只是它能使用的最大带宽资源，你不能让 Kafka 服务器常规性使用这么多资源，
>
> 故通常要再额外预留出 2/3 的资源，即单台服务器使用带宽 700Mb / 3 ≈ 240Mbps。需
>
> 要提示的是，这里的 2/3 其实是相当保守的，你可以结合你自己机器的使用情况酌情减少
>
> 此值。



## 监控

kafka producer速率监控jmx指标：

```
kafka.producer:type=[consumer|producer|connect]-node-metrics,client-id=
([-.\w]+),node-id=([0-9]+)
```



## 配置

### 存储

| 参数     | 说明                                     |      |
| -------- | ---------------------------------------- | ---- |
| log.dirs | 指定了broker需要使用的若干个文件目录路径 |      |
| log.dir  | 补充log.dirs,一般只需要配置log.dirs即可  |      |

> 线上生产环境一定要为`log.dirs`配置多个路径，如果有条件保证这些目录挂载到不同的物理磁盘上。
>
> 好处：
>
> - 提升读写性能
> - 能够实现故障转移， failover

### zookeeper

负责协调管理并保存kafka集群的所有元数据信息

| 参数              | 说明                                   |
| ----------------- | -------------------------------------- |
| zookeeper.connect | csv格式参数，zk1:2181,zk2:2181         |
| chroot            | 让多个kafka集群使用同一套zookeeper集群 |

### broker

| 参数                 | 说明                                                         |
| -------------------- | ------------------------------------------------------------ |
| listeners            | 监听器，告诉外部连接者通过什么协议访问指定主机名和端口开放的kafka服务 |
| advertised.listeners | 这组监听器是broker用于对外发布的                             |

### topic

| 参数                           | 说明                                                         |
| ------------------------------ | ------------------------------------------------------------ |
| auto.create.topics.enable      | 是否允许自动创建topic，建议设置成false，不然容易出现很多名字稀奇古怪的topic |
| unclean.leader.election.enable | 是否允许Unclean Leader选举， 是否允许保存的数据落后太多的副本竞选leader，建议false |
| auto.leader.rebalance.enable   | 是否允许定期进行leader选举， 似乎没什么人提，但是对生产环境影响非常大。true：允许kafka定期地对一些topic分区进行leader重选举，建议false了 |

### 数据留存

| 参数                              | 说明                                                         |
| --------------------------------- | ------------------------------------------------------------ |
| log.retention.{hour\|minutes\|ms} | 控制一条消息数据被保存多长时间。                             |
| log.retention.bytes               | 指定broker为消息保存的总磁盘容量大小，默认值为-1.            |
| message.max.bytes                 | 控制broker能够接收的最大消息大小，默认1000012太少了，还不到1MB，调大一点比较保险 |

### topic

| 参数              | 说明                                                         |
| ----------------- | ------------------------------------------------------------ |
| retention.ms      | 规定该topic消息被保存的时长，默认是7天，一旦设置了这个值，会覆盖掉broker端的全参数值 |
| retention.bytes   | 规定topic预留多大的磁盘空间                                  |
| max.message.bytes | 决定了kafka broker能够正常接收该topic最大消息大小            |

topic级别设置方式

- 创建topic时进行设置
- 修改topic时设置

```bash
bin/kafka-topics.sh--bootstrap-serverlocalhost:9092--create--topictransaction--partitions1--replication-factor1--configretention.ms=15552000000--configmax.message.bytes=5242880
```

```bash
bin/kafka-configs.sh--zookeeperlocalhost:2181--entity-typetopics--entity-nametransaction--alter--add-configmax.message.bytes=10485760
```



### JVM

> 说到 JVM 端设置，堆大小这个参数至关重要。虽然在后面我们还会讨论如何调优 Kafka 性能的问题，但现在我想无脑给出一个通用的建议：将你的 JVM 堆大小设置成 6GB 吧，这是目前业界比较公认的一个合理值。我见过很多人就是使用默认的 Heap Size 来跑 Kafka，说实话默认的 1GB 有点小，毕竟 Kafka Broker 在与客户端进行交互时会在 JVM 堆上创建大量的 ByteBuffer 实例，Heap Size 不能太小

现在我们确定好了要设置的 JVM 参数，我们该如何为 Kafka 进行设置呢？有些奇怪的是，这个问题居然在 Kafka 官网没有被提及。其实设置的方法也很简单，你只需要设置下面这两个环境变量即可：

- `KAFKA_HEAP_OPTS`：指定堆大小。
- `KAFKA_JVM_PERFORMANCE_OPTS`：指定 GC 参数。

比如你可以这样启动 Kafka Broker，即在启动 Kafka Broker 之前，先设置上这两个环境变量：

```bash
$> export KAFKA_HEAP_OPTS=--Xms6g  --Xmx6g
$> export  KAFKA_JVM_PERFORMANCE_OPTS= -server -XX:+UseG1GC -XX:MaxGCPauseMillis=20 -XX:InitiatingHeapOccupancyPercent=35 -XX:+ExplicitGCInvokesConcurrent -Djava.awt.headless=true
$> bin/kafka-server-start.sh config/server.properties
```



### 操作系统参数

通常情况下，Kafka 并不需要设置太多的 OS 参数，但有些因素最好还是关注一下，比如下面这几个：

- 文件描述符限制
- 文件系统类型
- Swappiness
- 提交时间

