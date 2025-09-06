package io.easytx.interceptor;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import io.easytx.annotation.Read;
import io.easytx.annotation.TransactionConfiguration;
import io.easytx.annotation.TxRead;
import io.easytx.annotation.TxWrite;
import io.easytx.annotation.Write;
import io.easytx.interceptor.executor.BaseExecutor;
import io.easytx.interceptor.executor.Executor;
import io.easytx.interceptor.executor.MicrometerExecutor;
import io.easytx.metrics.Metrics;
import io.easytx.routing.RoutingDataSource;
import io.easytx.service.TransactionService;

@Aspect
@Component
public class EasyTxAspect {

    private final TransactionService transactionService;
    private final Metrics metrics;

    public EasyTxAspect(TransactionService transactionService, Metrics metrics) {
        this.transactionService = transactionService;
        this.metrics = metrics;
    }

    @Around("@annotation(txRead)")
    public Object aroundTxRead(ProceedingJoinPoint pjp, TxRead txRead) {
        TransactionConfiguration transactionConfig = new TransactionConfiguration(txRead);
        Executor executor = getExecutorByFlag(pjp, transactionConfig.useMicrometer());
        return transactionService.read(() -> executor.execute(txRead.logLevel()),
                transactionConfig);
    }

    @Around("@annotation(txWrite)")
    public Object aroundTxWrite(ProceedingJoinPoint pjp, TxWrite txWrite) {
        TransactionConfiguration transactionConfig = new TransactionConfiguration(txWrite);
        Executor executor = getExecutorByFlag(pjp, transactionConfig.useMicrometer());
        return transactionService.write(() -> executor.execute(txWrite.logLevel()),
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
            BaseExecutor baseExecutor = new BaseExecutor(pjp);
            return baseExecutor.execute();
        } finally {
            RoutingDataSource.clear();
        }
    }

    private Executor getExecutorByFlag(ProceedingJoinPoint pjp, boolean useMicrometer) {
        if (useMicrometer) {
            return new MicrometerExecutor(metrics, pjp);
        } else {
            return new BaseExecutor(pjp);
        }
    }
}
