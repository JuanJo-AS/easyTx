package io.easytx.metrics;

import java.util.concurrent.TimeUnit;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

public class Metrics {

    private final MeterRegistry registry;

    public Metrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordTransactionTime(String type, long nanos) {
        if (registry != null) {
            Timer.builder("txlens").tag("type", type).register(registry).record(nanos,
                    TimeUnit.NANOSECONDS);
        }
    }

    public void incrementErrors(String type) {
        if (registry != null) {
            Counter.builder("txlens.errors").tag("type", type).register(registry).increment();
        }
    }
}
