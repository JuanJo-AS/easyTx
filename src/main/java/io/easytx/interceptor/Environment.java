package io.easytx.interceptor;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;

public class Environment {

    private String className;
    private String methodName;

    public Environment(ProceedingJoinPoint pjp) {
        Signature signature = pjp.getSignature();
        this.className = signature.getDeclaringTypeName();
        this.methodName = signature.getName();
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

}
