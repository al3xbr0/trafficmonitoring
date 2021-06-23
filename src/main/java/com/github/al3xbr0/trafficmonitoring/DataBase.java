package com.github.al3xbr0.trafficmonitoring;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;

import java.sql.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

import static com.github.al3xbr0.trafficmonitoring.TrafficLimit.LimitName.MAX;
import static com.github.al3xbr0.trafficmonitoring.TrafficLimit.LimitName.MIN;

public class DataBase {
    private static final Logger LOGGER = LogManager.getLogger(DataBase.class);

    private static final String SCHEMA_TABLE = "traffic_limits.limits_per_hour";
    private static final String NAME_COLUMN = "limit_name";
    private static final String VALUE_COLUMN = "limit_value";
    private static final String DATE_COLUMN = "effective_date";

    private static final String CHANNEL_NAME = "limits_updated";
    private static final int LISTEN_FREQUENCY_SECONDS = 5;

    private final Connection connection;

    public DataBase(String url, String user, String password) throws SQLException {
        connection = DriverManager.getConnection(url, user, password);
    }

    public void enableListeningToUpdates(Consumer<TrafficLimit.LimitName> limitUpdater) {
        PGConnection pgConnection = (PGConnection) connection;

        limitUpdater.accept(MAX);
        limitUpdater.accept(MIN);

        TimerTask poll =
                new TimerTask() {
                    @Override
                    public void run() {
                        try (Statement statement = connection.createStatement()) {
                            statement.execute("");
                            PGNotification[] notifications = pgConnection.getNotifications();
                            if (notifications != null) {
                                for (PGNotification notification : notifications) {
                                    limitUpdater.accept(TrafficLimit.LimitName.getLimit(notification.getParameter()));
                                }
                            }
                        } catch (SQLException e) {
                            LOGGER.error("Couldn't check for notifications", e);
                        }
                    }
                };

        try (Statement statement = connection.createStatement()) {
            statement.execute("LISTEN " + CHANNEL_NAME + ";");
        } catch (SQLException e) {
            LOGGER.error("Couldn't subscribe for notifications", e);
        }
        Timer pollTimer = new Timer(true);
        pollTimer.schedule(poll, 0, LISTEN_FREQUENCY_SECONDS * 1000L);
    }

    public TrafficLimit getLatestLimit(TrafficLimit.LimitName limitName) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            ResultSet result = statement.executeQuery("SELECT * FROM " + SCHEMA_TABLE +
                    " WHERE " + NAME_COLUMN + " = '" + limitName + "' ORDER BY " + DATE_COLUMN + " DESC LIMIT 1");
            result.next();
            return new TrafficLimit(limitName, result.getInt(VALUE_COLUMN), result.getDate(DATE_COLUMN));
        }
    }
}
