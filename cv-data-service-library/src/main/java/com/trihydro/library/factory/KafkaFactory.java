package com.trihydro.library.factory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.trihydro.library.helpers.Utility;

@Component
public class KafkaFactory {
    private final Utility utility;
    private final String kafkaType;
    private final Properties kafkaProperties;

    @Autowired
    public KafkaFactory(Utility _utility) throws IllegalArgumentException {
        utility = _utility;
        kafkaType = Optional.ofNullable(System.getenv("KAFKA_TYPE")).orElse("LOCAL");
        if ("CONFLUENT".equalsIgnoreCase(kafkaType)) {
            kafkaProperties = addConfluentProperties(new Properties());
        } else {
            kafkaProperties = new Properties();
        }
    }

    /**
     * Creates a Kafka consumer that has a key.deserializer and value.deserializer
     * of type StringDeserializer
     * 
     * @param host          URI or IP of a broker within the Kafka Cluster
     * @param consumerGroup Name of group this consumer should join
     * @param topic         Topic to subscribe to
     * @return A new string consumer
     */
    public Consumer<String, String> createStringConsumer(String host, String consumerGroup, String topic) {
        return createStringConsumer(host, consumerGroup, Arrays.asList(topic), null, null);
    }

    /**
     * Creates a Kafka consumer that has a key.deserializer and value.deserializer
     * of type StringDeserializer
     * 
     * @param host            URI or IP of a broker within the Kafka Cluster
     * @param consumerGroup   Name of group this consumer should join
     * @param topic           Topic to subscribe to
     * @param maxPollInterval The maximum delay between invocations of poll(), in
     *                        milliseconds, before the consumer is considered failed
     * @param maxPollRecords  The maximum number of records returned in a single
     *                        call to poll()
     * @return A new string consumer
     */
    public Consumer<String, String> createStringConsumer(String host, String consumerGroup, String topic,
            Integer maxPollInterval, Integer maxPollRecords) {
        return createStringConsumer(host, consumerGroup, Arrays.asList(topic), maxPollInterval, maxPollRecords);
    }

    /**
     * Creates a Kafka consumer that has a key.deserializer and value.deserializer
     * of type StringDeserializer
     * 
     * @param host          URI or IP of a broker within the Kafka Cluster
     * @param consumerGroup Name of group this consumer should join
     * @param topics        Topics to subscribe to
     * @return A new string consumer
     */
    public Consumer<String, String> createStringConsumer(String host, String consumerGroup, List<String> topics) {
        return createStringConsumer(host, consumerGroup, topics, null, null);
    }

    /**
     * Creates a Kafka consumer that has a key.deserializer and value.deserializer
     * of type StringDeserializer
     * 
     * @param host            URI or IP of a broker within the Kafka Cluster
     * @param consumerGroup   Name of group this consumer should join
     * @param topics          Topics to subscribe to
     * @param maxPollInterval The maximum delay between invocations of poll(), in
     *                        milliseconds, before the consumer is considered failed
     * @param maxPollRecords  The maximum number of records returned in a single
     *                        call to poll()
     * @return A new string consumer
     */
    public Consumer<String, String> createStringConsumer(String host, String consumerGroup, List<String> topics,
            Integer maxPollInterval, Integer maxPollRecords) {
        // Guard clauses. maxPollInterval and maxPollRecords are optional.
        if (StringUtils.isBlank(host))
            throw new IllegalArgumentException("host must be a non-null, non-empty string");

        if (StringUtils.isBlank(consumerGroup))
            throw new IllegalArgumentException("consumerGroup must be a non-null, non-empty string");

        if (topics == null || topics.size() == 0)
            throw new IllegalArgumentException("topics must be non-null and contain at least one topic");

        var props = new Properties();
        props.put("bootstrap.servers", host);
        props.put("group.id", consumerGroup);
        props.put("auto.commit.interval.ms", "1000");
        props.put("session.timeout.ms", "30000");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        // Add optional properties
        if (maxPollInterval != null) {
            props.put("max.poll.interval.ms", maxPollInterval.intValue());
        }

        if (maxPollRecords != null) {
            props.put("max.poll.records", maxPollRecords.intValue());
        }

        props.putAll(kafkaProperties);

        var consumer = new KafkaConsumer<String, String>(props);
        consumer.subscribe(topics);

        utility.logWithDate(String.format("Created consumer for consumer group %s, subscribed to topic(s) %s",
                consumerGroup, String.join(", ", topics)));

        return consumer;
    }

    /**
     * Creates a Kafka producer that has a key.deserializer and value.deserializer
     * of type StringDeserializer
     * 
     * @param host URI or IP of a broker within the Kafka Cluster
     * @return A new string producer
     */
    public Producer<String, String> createStringProducer(String host) {
        Properties props = new Properties();
        props.put("bootstrap.servers", host);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        props.putAll(kafkaProperties);

        return new KafkaProducer<>(props);
    }

    private Properties addConfluentProperties(Properties props) {
        String username = System.getenv("CONFLUENT_KEY");
        String password = System.getenv("CONFLUENT_SECRET");

        if (Strings.isNullOrEmpty(username) || Strings.isNullOrEmpty(password)) {
            throw new IllegalArgumentException("CONFLUENT_KEY and CONFLUENT_SECRET must be set in the environment");
        }

        String auth = "org.apache.kafka.common.security.plain.PlainLoginModule required " +
        "username=\"" + username + "\" " +
        "password=\"" + password + "\";";
        props.put("sasl.jaas.config", auth);
        props.put("ssl.endpoint.identification.algorithm", "https");
        props.put("security.protocol", "SASL_SSL");
        props.put("sasl.mechanism", "PLAIN");

        return props;
    }
}