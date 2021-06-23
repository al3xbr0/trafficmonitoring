package com.github.al3xbr0.trafficmonitoring;

import java.util.Properties;

public class AppProperties extends Properties {

    private static final String DB_URL = "database.url";
    private static final String DB_USER = "database.user";
    private static final String DB_PASSWORD = "database.password";

    private static final String CAPTURE_INTERVAL_MINUTES = "monitoring.captureIntervalMinutes";
    private static final String UPDATE_LIMITS_FREQUENCY_MINUTES = "monitoring.updateLimitsFrequencyMinutes";
    private static final String SOURCE_IP = "monitoring.sourceIp";
    private static final String DESTINATION_IP = "monitoring.destinationIp";

    private static final String SPARK_MASTER_URL = "spark.masterUrl";
    private static final String SPARK_DISABLE_DETAILED_LOGS = "spark.disableDetailedLogs";

    private static final String KAFKA_URL = "kafka.url";
    private static final String KAFKA_TOPIC = "kafka.topic";
    private static final String KAFKA_MESSAGE = "kafka.message";


    AppProperties() {
        super(12);
    }

    public String getDbUrl() {
        return getProperty(DB_URL);
    }

    public String getDbUser() {
        return getProperty(DB_USER);
    }

    public String getDbPassword() {
        return getProperty(DB_PASSWORD);
    }

    public int getCaptureIntervalMinutes() {
        return Integer.parseInt(getProperty(CAPTURE_INTERVAL_MINUTES));
    }

    public int getUpdateLimitsFrequencyMinutes() {
        return Integer.parseInt(getProperty(UPDATE_LIMITS_FREQUENCY_MINUTES));
    }

    public String getSourceIp() {
        return getProperty(DESTINATION_IP);
    }

    public String getDestinationIp() {
        return getProperty(SOURCE_IP);
    }

    public String getSparkMasterUrl() {
        return getProperty(SPARK_MASTER_URL);
    }

    public boolean isSparkDisableDetailedLogs() {
        return Boolean.parseBoolean(getProperty(SPARK_DISABLE_DETAILED_LOGS));
    }

    public String getKafkaUrl() {
        return getProperty(KAFKA_URL);
    }

    public String getKafkaTopic() {
        return getProperty(KAFKA_TOPIC);
    }

    public String getKafkaMessage() {
        return getProperty(KAFKA_MESSAGE);
    }
}
