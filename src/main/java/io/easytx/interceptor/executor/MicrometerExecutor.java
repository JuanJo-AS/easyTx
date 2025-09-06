package io.easytx.interceptor.executor;

import org.aspectj.lang.ProceedingJoinPoint;
import io.easytx.interceptor.Environment;
import io.easytx.metrics.Metrics;

public class MicrometerExecutor extends Executor {

    private static final ExecutorLogger LOGGER = createLogger(MicrometerExecutor.class);

    private final Metrics metrics;
    private final Environment env;
    private final String micrometerId;

    public MicrometerExecutor(Metrics metrics, ProceedingJoinPoint joinPoint) {
        super(joinPoint);
        this.metrics = metrics;
        this.env = new Environment(pjp);
        this.micrometerId = env.getClassName() + "." + env.getMethodName();
    }

    public Object executeWithAllLogs() {
        long start = System.nanoTime();
        Object result = executeWithWrapLogs();
        long durationNs = System.nanoTime() - start;
        metrics.recordTransactionTime(micrometerId, durationNs);
        LOGGER.took(durationNs);
        return result;
    }

    public Object executeWithWrapLogs() {
        long start = System.nanoTime();
        LOGGER.start(env);
        Object result = execute();
        LOGGER.finish(env);
        long durationNs = System.nanoTime() - start;
        metrics.recordTransactionTime(micrometerId, durationNs);
        return result;
    }

    public Object executeWithTimeLogs() {
        long start = System.nanoTime();
        Object result = execute();
        long durationNs = System.nanoTime() - start;
        metrics.recordTransactionTime(micrometerId, durationNs);
        LOGGER.took(durationNs);
        return result;
    }

    public Object execute() {
        try {
            return pjp.proceed();
        } catch (Throwable e) {
            metrics.incrementErrors(micrometerId);
            throw new RuntimeException(e);
        }
    }
}
