package io.easytx.interceptor.executor;

import org.slf4j.Logger;
import io.easytx.interceptor.Environment;

public class ExecutorLogger {

    private static final String TRANSACTION_STARTED = "{} -> {}: transaction started";
    private static final String TRANSACTION_FINISHED = "{} -> {}: transaction finished";
    private static final String TRANSACTION_TOOK = "Transaction took {} ms";

    private final Logger log;

    ExecutorLogger(Logger logger) {
        log = logger;
    }

    public void start(Environment env) {
        log(TRANSACTION_STARTED, env);
    }

    public void finish(Environment env) {
        log(TRANSACTION_FINISHED, env);
    }

    public void took(long nanoSeconds) {
        log(TRANSACTION_TOOK, getMilliseconds(nanoSeconds));
    }

    private void log(String message, Environment env) {
        log(message, env.getClassName(), env.getMethodName());
    }

    private void log(String message, Object... params) {
        log.info(message, params);
    }

    private long getMilliseconds(long nanoSeconds) {
        return nanoSeconds / 1_000_000;
    }
}
