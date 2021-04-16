# group rebalancing

> Consumer group rebalancing is triggered when partitions need to be reassigned among consumers in the consumer group: A new consumer joins the group; an existing consumer leaves the group; an existing consumer changes subscription; or partitions are added to one of the subscribed topics.

消费者组重平衡触发的几个场景

- 新的消费者加入到消费者组

- 消费者组中的消费者离开了

- 消费者组中的消费者改变了订阅

- 新增分区到topic中

- 当调用`poll`方法失败超过`max.poll.interval.ms`时间，会认为该消费者dead了，会触发重平衡

  > 如果是一个一条线程一个消费者的话而处理消息要花比较长的时间的话，这种触发场景会很常见



> 参考资料：
>
> 新的重平衡协议：https://www.confluent.io/blog/incremental-cooperative-rebalancing-in-kafka/



> 不管是什么版本的重平衡协议，当分区将要被回收的时候，消费者必须确保消息处理完并且该分区的offset已经提交后才会通知协调者该分区可以被安全分配了。



如果关闭了自动提交offset的话，就需要自己在发送`join group request`之前commit`offset`，有一下两种处理方式

- 在处理完数据之后并且在拉取下一批消息之前，调用`commitSync()`提交offset
- 实现`ConsumerRebalanceListener`去获取分区被回收的事件通知，在这个时候再去commit正确的offset





