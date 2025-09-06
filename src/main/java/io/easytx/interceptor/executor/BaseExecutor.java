package io.easytx.interceptor.executor;

import org.aspectj.lang.ProceedingJoinPoint;
import io.easytx.interceptor.Environment;

public class BaseExecutor extends Executor {

    private static final ExecutorLogger LOGGER = createLogger(BaseExecutor.class);

    public BaseExecutor(ProceedingJoinPoint joinPoint) {
        super(joinPoint);
    }

    public Object executeWithAllLogs() {
        long start = System.nanoTime();
        Object result = executeWithWrapLogs();
        long durationNs = System.nanoTime() - start;
        LOGGER.took(durationNs);
        return result;
    }

    public Object executeWithWrapLogs() {
        Environment env = new Environment(pjp);
        LOGGER.start(env);
        Object result = execute();
        LOGGER.finish(env);
        return result;
    }

    public Object executeWithTimeLogs() {
        long start = System.nanoTime();
        Object result = execute();
        long durationNs = System.nanoTime() - start;
        LOGGER.took(durationNs);
        return result;
    }

    public Object execute() {
        try {
            return pjp.proceed();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
