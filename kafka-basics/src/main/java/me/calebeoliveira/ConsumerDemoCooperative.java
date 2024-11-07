package me.calebeoliveira;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.CooperativeStickyAssignor;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class ConsumerDemoCooperative {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerDemoCooperative.class.getSimpleName());

    public static void main(String[] args) {
        LOGGER.info("I am a Kafka Consumer!");

        final String groupId = "my-java-application";
        final String topic = "demo_java";

        final Properties properties = new Properties();

        properties.setProperty("bootstrap.servers", "localhost:9092");

        // create consumer configs
        properties.setProperty("key.deserializer", StringDeserializer.class.getName());
        properties.setProperty("value.deserializer", StringDeserializer.class.getName());
        properties.setProperty("group.id", groupId);
        properties.setProperty("auto.offset.reset", "earliest");
        properties.setProperty("partition.assignment.strategy", CooperativeStickyAssignor.class.getName());
//        properties.setProperty("group.instance.id", "..."); // strategy for static assignments

        // create a consumer
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);

        // get a reference to the main thread
        final Thread mainThread = Thread.currentThread();

        // add a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Detected a shutdown, let's exit by calling consumer.wakeup()...");
            consumer.wakeup();

            // join the main thread to allow the execution of the code in the main thread
            try {
                mainThread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }));


        // subscribe to a topic

        try {
            consumer.subscribe(Arrays.asList(topic));

            // pool for data
            while (true) {
                LOGGER.info("Polling");
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));

                for (ConsumerRecord<String, String> record: records) {
                    LOGGER.info("Key: " + record.key() + ", Value: " + record.value());
                    LOGGER.info("Partition: " + record.partition() + ", Offset: " + record.offset());
                }
            }

        } catch (WakeupException e) {
            LOGGER.info("Consumer is starting to shutdown");
        } catch (Exception e) {
            LOGGER.error("Unexpected exception in the consumer");
        } finally {
            consumer.close(); // close the consumer, this will also commit offset
            LOGGER.info("The consumer is now gracefully shutdown");
        }

    }
}
