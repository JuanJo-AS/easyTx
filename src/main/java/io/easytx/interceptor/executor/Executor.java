package io.easytx.interceptor.executor;

import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.LoggerFactory;
import io.easytx.annotation.LogLevel;

public abstract class Executor {

    protected final ProceedingJoinPoint pjp;

    public abstract Object execute();

    public abstract Object executeWithAllLogs();

    public abstract Object executeWithWrapLogs();

    public abstract Object executeWithTimeLogs();

    protected Executor(ProceedingJoinPoint pjp) {
        this.pjp = pjp;
    }

    public Object execute(LogLevel logLevel) {
        switch (logLevel) {
            case OFF:
                return execute();
            case WRAP:
                return executeWithWrapLogs();
            case TIME:
                return executeWithTimeLogs();
            case ALL:
                return executeWithAllLogs();
            default:
                return execute();
        }
    }

    protected static ExecutorLogger createLogger(Class<?> clazz) {
        return new ExecutorLogger(LoggerFactory.getLogger(clazz));
    }
}
