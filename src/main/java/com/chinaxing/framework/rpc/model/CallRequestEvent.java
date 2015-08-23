package com.chinaxing.framework.rpc.model;

import java.lang.reflect.Method;

/**
 * Created by LambdaCat on 15/8/22.
 */
public class CallRequestEvent {
    private int id;
    private Class clz;
    private Method method;
    private Object[] arguments;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Class getClz() {
        return clz;
    }

    public void setClz(Class clz) {
        this.clz = clz;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Object[] getArguments() {
        return arguments;
    }

    public void setArguments(Object[] arguments) {
        this.arguments = arguments;
    }
}
