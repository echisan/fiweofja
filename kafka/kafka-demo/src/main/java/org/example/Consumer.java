package org.example;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.util.*;

public class Consumer {
    public static void main(String[] args) throws InterruptedException {

        Properties props = new Properties();
        props.put("bootstrap.servers", "172.28.138.11:9092");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("group.id", "test-group");
//        props.put("auto.offset.reset", "earliest");
        props.put("enable.auto.commit", "false");
//        props.put("auto.offset.reset", "earliest");
        props.put("auto.offset.reset", "latest");
        props.put("max.poll.records", 1);
//        props.put("max.poll.interval.ms", "5");
        List<String> interceptors = Collections.singletonList("org.example.MyConsumerInterceptor");
        props.put(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG, interceptors);

        KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(props);

        consumer.subscribe(Collections.singletonList("auto-offset-reset-test"));

        Set<TopicPartition> topicPartitions = new HashSet<>();
        TopicPartition tp = new TopicPartition("auto-offset-reset-test", 0);
        TopicPartition tp1 = new TopicPartition("quickstart-events", 0);
        topicPartitions.add(tp);
        topicPartitions.add(tp1);
        Map<TopicPartition, OffsetAndMetadata> committed = consumer.committed(topicPartitions);
        System.out.println(committed);

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(5));
//            System.out.println("records size:"+records.count());
            if (!records.isEmpty()) {
                for (ConsumerRecord<String, String> record : records) {
                    System.out.println(record.value());
                }
//                consumer.commitSync();
//                consumer.close();
//                break;
            }
        }
//        consumer.close();
    }


}
