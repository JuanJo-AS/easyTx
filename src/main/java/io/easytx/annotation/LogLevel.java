package io.easytx.annotation;

public enum LogLevel {
    OFF, WRAP, TIME, ALL;

    public boolean logWrap() {
        return this == WRAP || this == ALL;
    }

    public boolean logTime() {
        return this == TIME || this == ALL;
    }
}
