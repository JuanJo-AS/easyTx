package io.easytx.interceptor;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import io.easytx.annotation.LogLevel;
import io.easytx.annotation.Read;
import io.easytx.annotation.TransactionConfiguration;
import io.easytx.annotation.TxRead;
import io.easytx.annotation.TxWrite;
import io.easytx.annotation.Write;
import io.easytx.metrics.Metrics;
import io.easytx.routing.RoutingDataSource;
import io.easytx.service.TransactionService;

@Aspect
@Component
public class EasyTxAspect {

    private final TransactionService transactionService;
    private final Metrics metrics;
    private static final Logger LOGGER = LoggerFactory.getLogger(EasyTxAspect.class);

    public EasyTxAspect(TransactionService transactionService, Metrics metrics) {
        this.transactionService = transactionService;
        this.metrics = metrics;
    }

    @Around("@annotation(txRead)")
    public Object aroundTxRead(ProceedingJoinPoint pjp, TxRead txRead) {
        TransactionConfiguration transactionConfig = new TransactionConfiguration(txRead);
        return transactionService.withReadTransaction(
                () -> executeWithMicrometer(pjp, txRead.logLevel(), "read transaction"),
                transactionConfig);
    }

    @Around("@annotation(txWrite)")
    public Object aroundTxWrite(ProceedingJoinPoint pjp, TxWrite txWrite) {
        TransactionConfiguration transactionConfig = new TransactionConfiguration(txWrite);
        return transactionService.withWriteTransaction(
                () -> executeWithMicrometer(pjp, txWrite.logLevel(), "write transaction"),
                transactionConfig);
    }

    @Around("@annotation(read)")
    public Object aroundRead(ProceedingJoinPoint pjp, Read read) {
        return route(pjp, "read");
    }

    @Around("@annotation(write)")
    public Object aroundRead(ProceedingJoinPoint pjp, Write write) {
        return route(pjp, "write");
    }

    private Object route(ProceedingJoinPoint pjp, String dataSourceKey) {
        try {
            RoutingDataSource.setDataSourceKey(dataSourceKey);
            return execute(pjp, null);
        } finally {
            RoutingDataSource.clear();
        }
    }


    private Object execute(ProceedingJoinPoint pjp, LogLevel logLevel) {
        long start = getStartTime(logLevel);
        Signature signature = pjp.getSignature();
        String className = signature.getDeclaringTypeName();
        String methodName = signature.getName();
        try {
            logWrap(logLevel, "{} -> {}: transaction started", className, methodName);
            return pjp.proceed();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            logWrap(logLevel, "{} -> {}: transaction finished", className, methodName);
            long durationNs = System.nanoTime() - start;
            logTime(logLevel, "Transaction took {} ms", getMilliseconds(durationNs));
        }
    }

    private Object executeWithMicrometer(ProceedingJoinPoint pjp, LogLevel logLevel, String type) {
        long start = getStartTime(logLevel);
        Signature signature = pjp.getSignature();
        String className = signature.getDeclaringTypeName();
        String methodName = signature.getName();
        try {
            logWrap(logLevel, "{} -> {}: transaction started", className, methodName);
            return pjp.proceed();
        } catch (Throwable e) {
            metrics.incrementErrors(type);
            throw new RuntimeException(e);
        } finally {
            logWrap(logLevel, "{} -> {}: transaction finished", className, methodName);
            long durationNs = System.nanoTime() - start;
            logTimeWithMicrometer(logLevel, "Transaction took {} ms", durationNs, type);
        }
    }

    private void logWrap(LogLevel level, String message, Object... params) {
        if (level.logWrap()) {
            LOGGER.info(message, params);
        }
    }

    private long getStartTime(LogLevel level) {
        if (level.logTime()) {
            return System.nanoTime();
        } else {
            return 0l;
        }
    }

    private void logTime(LogLevel level, String message, long seconds) {
        if (level.logTime()) {
            LOGGER.info(message, seconds);
        }
    }

    private void logTimeWithMicrometer(LogLevel level, String message, long nanoseconds,
            String type) {
        if (level.logTime()) {
            metrics.recordTransactionTime(type, nanoseconds);
            LOGGER.info(message, getMilliseconds(nanoseconds));
        }
    }

    private long getMilliseconds(long nanoSeconds) {
        return nanoSeconds / 1_000_000;
    }

}
