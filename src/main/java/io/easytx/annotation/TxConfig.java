package io.easytx.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface TxConfig {
    LogLevel logLevel() default LogLevel.OFF;

}
