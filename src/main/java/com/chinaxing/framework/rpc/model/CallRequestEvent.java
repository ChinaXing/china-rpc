package com.chinaxing.framework.rpc.model;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by LambdaCat on 15/8/22.
 */
public class CallRequestEvent {
    private String destination;
    private List<String> availableDestinations;
    private int id;
    private Class clz;
    private Method method;
    private Object[] arguments;

    private Exception exception;

    public void setException(Exception exception) {
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }

    public void setAvailableDestinations(List<String> availableDestinations) {
        this.availableDestinations = availableDestinations;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

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

    public List<String> getAvailableDestinations() {
        return availableDestinations;
    }
}
