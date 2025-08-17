package io.easytx.user.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.easytx.metrics.Metrics;
import io.micrometer.core.instrument.MeterRegistry;

@Configuration
public class TxLensConfig {

    @Bean
    public Metrics metrics(ObjectProvider<MeterRegistry> registry) {
        return new Metrics(registry.getIfAvailable());
    }
}
