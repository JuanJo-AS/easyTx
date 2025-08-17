package io.easytx.interceptor;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import io.easytx.annotation.LogLevel;
import io.easytx.annotation.TransactionConfiguration;
import io.easytx.annotation.TxRead;
import io.easytx.annotation.TxWrite;
import io.easytx.service.TransactionService;

@Aspect
@Component
public class TxLensAspect {

    private final TransactionService transactionService;
    private static final Logger LOGGER = LoggerFactory.getLogger(TxLensAspect.class);

    public TxLensAspect(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Around("@annotation(txRead)")
    public Object aroundTxRead(ProceedingJoinPoint pjp, TxRead txRead) {
        TransactionConfiguration transactionConfig = new TransactionConfiguration(txRead);
        return transactionService.withReadTransaction(() -> execute(pjp, txRead.logLevel()),
                transactionConfig);
    }

    @Around("@annotation(txWrite)")
    public Object aroundTxWrite(ProceedingJoinPoint pjp, TxWrite txWrite) {
        TransactionConfiguration transactionConfig = new TransactionConfiguration(txWrite);
        return transactionService.withWriteTransaction(() -> execute(pjp, txWrite.logLevel()),
                transactionConfig);
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
            logTime(logLevel, "Transaction took {} ms", getSeconds(durationNs));
        }
    }

    private void logWrap(LogLevel level, String message, Object... params) {
        if (level == LogLevel.WRAP || level == LogLevel.ALL) {
            LOGGER.info(message, params);
        }
    }

    private long getStartTime(LogLevel level) {
        if (level == LogLevel.TIME || level == LogLevel.ALL) {
            return System.nanoTime();
        } else {
            return 0l;
        }
    }

    private void logTime(LogLevel level, String message, Object... params) {
        if (level == LogLevel.TIME || level == LogLevel.ALL) {
            LOGGER.info(message, params);
        }
    }

    private long getSeconds(long nanoSeconds) {
        return nanoSeconds / 1_000_000;
    }

}
