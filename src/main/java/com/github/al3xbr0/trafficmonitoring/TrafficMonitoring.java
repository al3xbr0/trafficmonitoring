package com.github.al3xbr0.trafficmonitoring;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.util.NifSelector;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.sql.SQLException;
import java.util.Properties;
import java.util.TimerTask;

import static com.github.al3xbr0.trafficmonitoring.TrafficLimit.LimitName.MAX;
import static com.github.al3xbr0.trafficmonitoring.TrafficLimit.LimitName.MIN;

public class TrafficMonitoring {

    private static final Logger LOGGER = LogManager.getLogger(TrafficMonitoring.class);

    private static final Properties KAFKA_PROPERTIES = new Properties();

    static {
        KAFKA_PROPERTIES.setProperty("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        KAFKA_PROPERTIES.setProperty("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    }


    private DataBase dataBase;

    private TrafficLimit currentMax, currentMin;

    private final AppProperties props;

    private final String nifName;
    private final String filter;

    private final Producer<String, String> kafkaProducer;

    @Deprecated
    private final TimerTask limitsUpdateTask = new TimerTask() {
        @Override
        public void run() {
            try {
                currentMax = dataBase.getLatestLimit(MAX);
                currentMin = dataBase.getLatestLimit(MIN);
            } catch (SQLException e) {
                LOGGER.error("Couldn't read from DB", e);
            }
        }
    };

    public TrafficMonitoring(AppProperties props, String nifName) {
        try {
            dataBase = new DataBase(props.getDbUrl(), props.getDbUser(), props.getDbPassword());
        } catch (SQLException e) {
            LOGGER.error("Couldn't connect to DB", e);
            System.exit(1);
        }

        dataBase.enableListeningToUpdates(
                limitName -> {
                    try {
                        TrafficLimit lim = dataBase.getLatestLimit(limitName);
                        if (MAX.equals(limitName)) {
                            currentMax = lim;
                            LOGGER.info("Max limit is set to {}", lim.getLimitValue());
                        } else {
                            currentMin = lim;
                            LOGGER.info("Min limit is set to {}", lim.getLimitValue());
                        }
                    } catch (SQLException e) {
                        LOGGER.error("Couldn't read from DB", e);
                    }
                }
        );

        this.props = props;

/*      Task 3
        Timer limitsUpdateTimer = new Timer(true);
        limitsUpdateTimer.schedule(limitsUpdateTask, 0, 60 * 1000L * props.getUpdateLimitsFrequencyMinutes());*/

        this.nifName = nifName;
        filter = buildFilterStr();

        KAFKA_PROPERTIES.setProperty("bootstrap.servers", props.getKafkaUrl());
        kafkaProducer = new KafkaProducer<>(KAFKA_PROPERTIES);
    }

    public void startMonitoring() throws InterruptedException {
        SparkConf conf = new SparkConf().setMaster(props.getSparkMasterUrl()).setAppName(this.getClass().getSimpleName());
        JavaStreamingContext context = new JavaStreamingContext(conf, Durations.minutes(props.getCaptureIntervalMinutes()));
        if (props.isSparkDisableDetailedLogs()) {
            org.apache.log4j.LogManager.getLogger(conf.getClass().getPackageName()).setLevel(Level.ERROR);
        }

        JavaReceiverInputDStream<Integer> packetSizes = context.receiverStream(new PacketCustomReceiver(nifName, filter));
        JavaDStream<Integer> sizesSum = packetSizes.reduce(Integer::sum);

        sizesSum.foreachRDD(
                rdd -> {
                    if (rdd.isEmpty()) {
                        LOGGER.warn("No traffic captured");
                        return;
                    }
                    int trafficSize = rdd.first();
                    if (!isInsideLimits(trafficSize)) {
                        kafkaProducer.send(new ProducerRecord<>(props.getKafkaTopic(), props.getKafkaMessage()));
                        LOGGER.info("Traffic size is {}. Sending message to Kafka server at {}",
                                trafficSize, props.getKafkaUrl());
                    } else {
                        LOGGER.info("Captured traffic size {} is OK", trafficSize);
                    }
                }
        );

        context.start();
        context.awaitTermination();
    }

    private boolean isInsideLimits(int value) {
        return (currentMin.getLimitValue() <= value) && (value <= currentMax.getLimitValue());
    }

    private String buildFilterStr() {
        StringBuilder sb = new StringBuilder();
        String src = props.getSourceIp();
        String dst = props.getDestinationIp();
        if (!src.isEmpty()) {
            sb.append("src host ").append(src).append(" ");
        }
        if (!dst.isEmpty()) {
            sb.append("dst host ").append(dst);
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            LOGGER.error("Properties file not provided");
            return;
        }

        AppProperties properties = new AppProperties();
        try (Reader fis = new FileReader(args[0])) {
            properties.load(fis);
        } catch (IOException e) {
            LOGGER.error("Couldn't read properties from file {}", args[0]);
            return;
        }

        PcapNetworkInterface nif;
        try {
            nif = new NifSelector().selectNetworkInterface();
        } catch (IOException e) {
            LOGGER.error("Check your network interfaces", e);
            return;
        }
        if (nif == null) {
            return;
        }

        TrafficMonitoring trafficMonitoring = new TrafficMonitoring(properties, nif.getName());

        try {
            trafficMonitoring.startMonitoring();
        } catch (InterruptedException e) {
            LOGGER.error("Error during monitoring", e);
        }
    }
}
