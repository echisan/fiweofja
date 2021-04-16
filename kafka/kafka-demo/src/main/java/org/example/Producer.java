package org.example;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.io.IOException;
import java.util.Properties;

/**
 * Hello world!
 */
public class Producer {
    public static void main(String[] args) throws IOException {

        Properties props = new Properties();
        props.put("bootstrap.servers", "172.28.138.11:9092");
        props.put("acks", "all");
        props.put("retries", 0);
        props.put("linger.ms", 1);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        org.apache.kafka.clients.producer.Producer<String, String> producer = new KafkaProducer<>(props);
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
