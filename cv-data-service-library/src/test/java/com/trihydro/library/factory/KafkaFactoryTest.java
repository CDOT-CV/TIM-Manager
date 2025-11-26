package com.trihydro.library.factory;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.Producer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class KafkaFactoryTest {

  @InjectMocks
    private KafkaFactory kafkaFactory;

    @Test
    public void testCreateStringConsumerLocal() {
        String host = "localhost:9092";
        String consumerGroup = "testGroup";
        String topic = "testTopic";

        Consumer<String, String> consumer = kafkaFactory.createStringConsumer(host, consumerGroup, topic);

        assertNotNull(consumer);
    }

    @Test
    public void testKafkaFactoryConstructorConfluentException() throws Exception {
        KafkaFactory kafkaFactoryTest = new KafkaFactory();
        assertThrows(IllegalArgumentException.class, () -> {
            kafkaFactoryTest.addConfluentProperties(new Properties());
        });
    }

    @Test
    public void testKafkaFactoryConstructorConfluent() {
        // Create a spy of KafkaFactory
        KafkaFactory spyKafkaFactory = spy(new KafkaFactory());

        // Mock the getKafka() method
        doReturn("testKey").when(spyKafkaFactory).getConfluentKey();
        doReturn("testSecret").when(spyKafkaFactory).getConfluentSecret();

        Properties props = spyKafkaFactory.addConfluentProperties(new Properties());

        assertNotNull(props);
        assertEquals("org.apache.kafka.common.security.plain.PlainLoginModule required username=\"testKey\" password=\"testSecret\";", props.getProperty("sasl.jaas.config"));
        assertEquals("https", props.getProperty("ssl.endpoint.identification.algorithm"));
        assertEquals("SASL_SSL", props.getProperty("security.protocol"));
        assertEquals("PLAIN", props.getProperty("sasl.mechanism"));
    }

    @Test
    public void testCreateStringConsumerWithProperties() {
        String host = "localhost:9092";
        String consumerGroup = "testGroup";
        List<String> topics = Arrays.asList("testTopic1", "testTopic2");
        Integer maxPollInterval = 300000;
        Integer maxPollRecords = 500;

        Consumer<String, String> consumer = kafkaFactory.createStringConsumer(host, consumerGroup, topics, maxPollInterval, maxPollRecords);

        assertNotNull(consumer);
    }

    @Test
    public void testCreateStringProducerLocal() {
        String host = "localhost:9092";

        Producer<String, String> producer = kafkaFactory.createStringProducer(host);

        assertNotNull(producer);
    }

    @Test
    public void testCreateStringConsumerLocalWithInvalidHost() {
        String host = "";
        String consumerGroup = "testGroup";
        String topic = "testTopic";

        assertThrows(IllegalArgumentException.class, () -> {
            kafkaFactory.createStringConsumer(host, consumerGroup, topic);
        });
    }

    @Test
    public void testCreateStringConsumerWithInvalidConsumerGroup() {
        String host = "localhost:9092";
        String consumerGroup = "";
        String topic = "testTopic";

        assertThrows(IllegalArgumentException.class, () -> {
            kafkaFactory.createStringConsumer(host, consumerGroup, topic);
        });
    }

    @Test
    public void testCreateStringConsumerWithInvalidTopics() {
        String host = "localhost:9092";
        String consumerGroup = "testGroup";
        List<String> topics = Arrays.asList();

        assertThrows(IllegalArgumentException.class, () -> {
            kafkaFactory.createStringConsumer(host, consumerGroup, topics);
        });
    }
}