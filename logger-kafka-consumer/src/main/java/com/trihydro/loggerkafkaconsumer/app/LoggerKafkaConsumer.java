package com.trihydro.loggerkafkaconsumer.app;

import com.trihydro.library.helpers.DateTimeHelper;
import com.trihydro.library.helpers.DateTimeHelperImpl;

import java.time.Instant;
import java.util.Date;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.trihydro.library.factory.KafkaFactory;
import com.trihydro.library.helpers.EmailHelper;
import com.trihydro.library.model.ActiveTim;
import com.trihydro.library.model.CertExpirationModel;
import com.trihydro.library.model.TopicDataWrapper;
import com.trihydro.loggerkafkaconsumer.app.dataConverters.TimDataConverter;
import com.trihydro.loggerkafkaconsumer.app.services.ActiveTimHoldingService;
import com.trihydro.loggerkafkaconsumer.app.services.ActiveTimService;
import com.trihydro.loggerkafkaconsumer.app.services.TimService;
import com.trihydro.loggerkafkaconsumer.config.LoggerConfiguration;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import us.dot.its.jpo.ode.model.OdeData;

@Component
@Slf4j
@Import(DateTimeHelperImpl.class)
public class LoggerKafkaConsumer {

    private final LoggerConfiguration loggerConfig;
    private final KafkaFactory kafkaFactory;
    private final ActiveTimService activeTimService;
    private final ActiveTimHoldingService activeTimHoldingService;
    private final TimService timService;
    private final TimDataConverter timDataConverter;
    private final EmailHelper emailHelper;
    private final DateTimeHelper dateTimeHelper;
    
    private final Gson gson = new Gson();

    @Autowired
    public LoggerKafkaConsumer(LoggerConfiguration _loggerConfig, KafkaFactory _kafkaFactory,
                               ActiveTimService _activeTimService, TimService _timService,
                               TimDataConverter _timDataConverter, EmailHelper _emailHelper,
                               ActiveTimHoldingService _activeTimHoldingService, DateTimeHelper dateTimeHelper) throws Exception {
        loggerConfig = _loggerConfig;
        kafkaFactory = _kafkaFactory;
        activeTimService = _activeTimService;
        timService = _timService;
        timDataConverter = _timDataConverter;
        emailHelper = _emailHelper;
        activeTimHoldingService = _activeTimHoldingService;
        this.dateTimeHelper = dateTimeHelper;

        log.info("Logger Kafka Consumer starting..............");

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        startKafkaConsumer();
    }

    /**
     * Starts the Kafka consumer to process messages from configured topics.
     * Handles TIM messages, driver alerts, and certificate expiration messages.
     *
     * @throws Exception If there is an issue starting or running the consumer
     */
    public void startKafkaConsumer() throws Exception {
        log.info("Starting Kafka consumer at {}:9092 for group {} and topic {}", loggerConfig.getKafkaHostServer(), loggerConfig.getDepositGroup(),
            loggerConfig.getDepositTopic());

        String endpoint = loggerConfig.getKafkaHostServer() + ":9092";
        var stringConsumer = kafkaFactory.createStringConsumer(endpoint, loggerConfig.getDepositGroup(), loggerConfig.getDepositTopic(),
            loggerConfig.getMaxPollIntervalMs(), loggerConfig.getMaxPollRecords());

        log.debug("Kafka consumer configuration: maxPollIntervalMs={}, maxPollRecords={}", loggerConfig.getMaxPollIntervalMs(),
            loggerConfig.getMaxPollRecords());

        try {
            var recordCount = 0;
            log.info("Kafka consumer loop started, waiting for messages...");

            while (true) {
                ConsumerRecords<String, String> records = stringConsumer.poll(100);
                recordCount = records.count();

                if (recordCount > 0) {
                    log.info("Polling found {} new record(s) to process", recordCount);
                }

                for (ConsumerRecord<String, String> record : records) {
                    log.debug("Processing record from partition={}, offset={}", record.partition(), record.offset());

                    TopicDataWrapper tdw;
                    try {
                        tdw = gson.fromJson(record.value(), TopicDataWrapper.class);
                        log.trace("Deserialized JSON to TopicDataWrapper");
                    } catch (Exception e) {
                        log.error("Failed to parse record from partition={}, offset={}: {}", record.partition(), record.offset(), e.getMessage());
                        log.debug("Problematic record content: {}", record.value());
                        continue;
                    }

                    if (tdw != null && tdw.getData() != null) {
                        log.info("Processing message for topic: {}", tdw.getTopic());

                        try {
                            switch (tdw.getTopic()) {
                                case "topic.OdeTimJson":
                                    processOdeTimJson(tdw);
                                    break;

                                case "topic.OdeTIMCertExpirationTimeJson":
                                    processOdeTIMCertExpirationTimeJson(tdw);
                                    break;

                                default:
                                    log.warn("Unhandled topic: {}", tdw.getTopic());
                            }
                        } catch (Exception e) {
                            log.error("Error processing message for topic {}: {}", tdw.getTopic(), e.getMessage(), e);
                        }
                    } else {
                        log.error("Deserialization failed - received invalid TopicDataWrapper");
                        if (tdw != null) {
                            log.debug("Partial deserialization - wrapper exists but data is null");
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Critical error in Kafka consumer main loop: {}", ex.getMessage(), ex);

            emailHelper.ContainerRestarted(loggerConfig.getAlertAddresses(), loggerConfig.getMailPort(), loggerConfig.getMailHost(),
                loggerConfig.getFromEmail(), "Logger Kafka Consumer");
            throw ex;
        } finally {
            log.info("Closing Kafka consumer connection");
            try {
                stringConsumer.close();
                log.info("Kafka consumer closed successfully");
            } catch (Exception consumerEx) {
                log.error("Failed to close Kafka consumer cleanly: {}", consumerEx.getMessage(), consumerEx);
            }
        }
    }

    /**
     * Process messages from the OdeTimJson topic
     */
    private void processOdeTimJson(TopicDataWrapper tdw) {
        log.trace("Starting OdeTimJson processing for message with length: {} bytes", tdw.getData().length());
        log.trace("Message content: {}", tdw.getData());

        OdeData odeData = timDataConverter.processTimJson(tdw.getData());

        if (odeData == null) {
            log.error("Failed to parse topic.OdeTimJson message, database insertion skipped");
            return;
        }

        log.trace("Successfully parsed OdeTimJson into OdeData object: {}", gson.toJson(odeData));

        try {
            if (odeData.getMetadata().getRecordGeneratedBy() == us.dot.its.jpo.ode.model.OdeMsgMetadata.GeneratedBy.TMC) {
                log.debug("Processing TIM generated by TMC");
                timService.addActiveTimToDatabase(odeData);
                log.debug("Successfully added active TIM to database");
            } else if (odeData.getMetadata().getRecordGeneratedBy() == null) {
                log.error("Failed to get recordGeneratedBy from metadata, defaulting to standard TIM processing");
                timService.addTimToDatabase(odeData); // TODO: identify if this method call can be removed
            } else {
                log.debug("Processing standard TIM with recordGeneratedBy: {}", odeData.getMetadata().getRecordGeneratedBy());
                timService.addTimToDatabase(odeData);
                log.debug("Successfully added TIM to database");
            }
        } catch (Exception e) {
            log.error("Exception while processing OdeTimJson: {}", e.getMessage(), e);
        }
    }

    /**
     * Process messages from the OdeTIMCertExpirationTimeJson topic
     */
    private void processOdeTIMCertExpirationTimeJson(TopicDataWrapper tdw) {
        log.debug("Starting OdeTIMCertExpirationTimeJson processing");

        try {
            CertExpirationModel certExpirationModel = gson.fromJson(tdw.getData(), CertExpirationModel.class);
            log.debug("Processing certificate expiration for packet ID: {}", certExpirationModel.getPacketID());

            var success = timService.updateActiveTimExpiration(certExpirationModel);

            if (success) {
                log.info("Successfully updated expiration date for packet ID: {}", certExpirationModel.getPacketID());
                return;
            }

            // Handle failure cases when the update was not successful
            log.debug("Initial update attempt failed, checking for special cases");

            // Check if active TIM exists
            var activeTim = activeTimService.getActiveTimByPacketId(certExpirationModel.getPacketID());

            if (activeTim == null) {
                // Active TIM not found, try the holding table
                log.debug("Active TIM not found for packet ID: {}, checking holding table", certExpirationModel.getPacketID());

                var ath = activeTimHoldingService.getActiveTimHoldingByPacketId(certExpirationModel.getPacketID());

                if (ath != null) {
                    log.debug("Found record in holding table, updating expiration");
                    success = activeTimHoldingService.updateTimExpiration(certExpirationModel.getPacketID(), Instant.parse(certExpirationModel.getExpirationDate()));

                    if (success) {
                        log.info("Successfully updated expiration date in holding table for packet ID: {}", certExpirationModel.getPacketID());
                    } else {
                        log.warn("Failed to update expiration date in holding table for packet ID: {}", certExpirationModel.getPacketID());
                    }
                } else {
                    log.warn("No record found in active TIM or holding tables for packet ID: {}", certExpirationModel.getPacketID());
                }
            } else if (messageSuperseded(certExpirationModel.getStartDateTime(), activeTim)) {
                // Message is superseded by newer data
                log.info("Unable to update expiration date for Active TIM {} (Packet ID: {}). Message superseded by newer data.",
                    activeTim.getActiveTimId(), certExpirationModel.getPacketID());
                success = true; // Consider this a success case since no action is needed
            }

            if (!success) {
                // Final failure case after all recovery attempts
                log.error("Failed to update expiration for packet ID: {}, expiration date: {}", certExpirationModel.getPacketID(),
                    certExpirationModel.getExpirationDate());

                String body = "logger-kafka-consumer failed attempting to update the expiration for an ActiveTim record";
                body += "<br/>";
                body += "The associated expiration topic record is: <br/>";
                body += tdw.getData();

                emailHelper.SendEmail(loggerConfig.getAlertAddresses(), "Failed To Update ActiveTim Expiration", body);
            }
        } catch (Exception ex) {
            log.error("Failed to parse topic.OdeTIMCertExpirationTimeJson: {}", ex.getMessage(), ex);
        }
    }

    private boolean messageSuperseded(String startTime, ActiveTim dbRecord) {
        try {
            Date expectedStart = dateTimeHelper.convertDate(startTime);

            if (expectedStart == null || dbRecord.getStartTimestamp() == null) {
                return false;
            }

            // If db record bas a start time that's later than the cert expiration model's
            // start time,
            // then the TIM in question must have been updated, and the cert expiration
            // model we're
            // currently processing has been superseded.
            return expectedStart.getTime() < dbRecord.getStartTimestamp().getTime();
        } catch (Exception ex) {
            log.error("Error while checking if message was superseded: {}", ex.getMessage());
            return false;
        }
    }
}