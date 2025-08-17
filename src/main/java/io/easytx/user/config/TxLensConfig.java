package io.easytx.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.easytx.metrics.Metrics;
import io.micrometer.core.instrument.MeterRegistry;

@Configuration
public class TxLensConfig {

    @Bean
    public Metrics metrics(MeterRegistry registry) {
        return new Metrics(registry);
    }
}
