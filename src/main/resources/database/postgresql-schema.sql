CREATE SCHEMA IF NOT EXISTS traffic_limits;
CREATE TABLE IF NOT EXISTS traffic_limits.limits_per_hour
(
    limit_name     varchar(32) NOT NULL,
    limit_value    int         NOT NULL,
    effective_date timestamp   NOT NULL
);


CREATE OR REPLACE FUNCTION traffic_limits.limits_updated_notify()
    RETURNS TRIGGER AS
$$
DECLARE
BEGIN
    PERFORM pg_notify('limits_updated', NEW.limit_name);
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER limits_per_hour_update
    AFTER INSERT
    ON traffic_limits.limits_per_hour
    FOR EACH ROW
EXECUTE FUNCTION traffic_limits.limits_updated_notify();
