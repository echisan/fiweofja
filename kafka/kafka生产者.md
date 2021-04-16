# 生产者

## 生产者分区策略

分区策略：决定生产者将消息发到哪个分区的算法。

- 轮询
- 随机
- 按消息键(可以根据key做hash取模 )



## 生产者压缩算法

kafka是如何压缩消息的？

kafka的消息层次都分为两层：消息集合(message set)以及消息(message)，一个消息集合中包含若干条日志项(record item)，而日志项才是真正封装消息的地方。kafka底层的消息日志由一系列消息集合日志项组成。kafka通常不会直接操作具体的一条条消息，总会在消息集合这个层面上进行写入操作。



目前消息格式有两个版本，V1,V2

V1：每条消息都需要执行CRC校验，但是CRC的值会相应更新。

V2：保存压缩消息的方法改变了。之前 V1 版本中保存压缩消息的方法是把多条消息进行压缩然后保存到外层消息的消息体字段中；而 V2 版本的做法是对整个消息集合进行压缩。显然后者应该比前者有更好的压缩效果。

![image-20210414153917324](../images/image-20210414153917324.png)



**何时压缩**

在 Kafka 中，压缩可能发生在两个地方：生产者端和 Broker 端。

产者程序中配置 compression.type 参数即表示启用指定类型的压缩算法。

broker端进行压缩的情况：

- Broker 端指定了和 Producer 端不同的压缩算法。
- Broker 端发生了消息格式转换。

**何时解压缩**

消息到达consumer端后，由consumer自行解压缩还原成之前的消息。kafka会将启用了哪种压缩算法封装进消息集合中。

broker端也会进行解压缩。每个压缩过的消息集合在 Broker 端写入时都要发生解压缩操作，目的就是为了对消息执行各种验证。



**Producer 端压缩、Broker 端保持、Consumer 端解压缩。**



## 使用

```java
package org.example;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.IOException;
import java.util.Properties;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) throws IOException {

        Properties props = new Properties();
        props.put("bootstrap.servers", "172.28.138.11:9092");
        props.put("acks", "all");
        props.put("retries", 0);
        props.put("linger.ms", 1);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        Producer<String, String> producer = new KafkaProducer<>(props);
        for (int i = 0; i < 100; i++){
            System.out.println("i:" + i);
            producer.send(new ProducerRecord<String, String>("quickstart-events", Integer.toString(i), Integer.toString(i)),
                    new Callback() {
                        @Override
                        public void onCompletion(RecordMetadata metadata, Exception exception) {
                            System.out.println(metadata.toString());
                            if (exception != null) {
                                exception.printStackTrace();
                            }
                        }
                    });
        }
        producer.close();
    }
}

```

