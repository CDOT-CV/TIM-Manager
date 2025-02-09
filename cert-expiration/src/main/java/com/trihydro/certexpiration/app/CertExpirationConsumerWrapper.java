package com.trihydro.certexpiration.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CertExpirationConsumerWrapper {
    @Autowired
    public CertExpirationConsumerWrapper(CertExpirationConsumer consumer) throws Exception {
        consumer.startKafkaConsumer();
    }
}
