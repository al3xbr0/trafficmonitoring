package com.github.al3xbr0.trafficmonitoring;

import java.util.Date;

public class TrafficLimit {

    public enum LimitName {
        MAX("max"),
        MIN("min");

        private final String name;

        LimitName(String name) {
            this.name = name;
        }

        public static LimitName getLimit(String name) {
            for (LimitName type : LimitName.values()) {
                if (type.name.equalsIgnoreCase(name)) {
                    return type;
                }
            }
            throw new IllegalArgumentException();
        }

        @Override
        public String toString() {
            return name;
        }
    }


    private final LimitName limitName;
    private final int limitValue;
    private final Date effectiveDate;

    public int getLimitValue() {
        return limitValue;
    }

    public TrafficLimit(LimitName limitName, int limitValue, Date effectiveDate) {
        this.limitName = limitName;
        this.limitValue = limitValue;
        this.effectiveDate = effectiveDate;
    }
}
