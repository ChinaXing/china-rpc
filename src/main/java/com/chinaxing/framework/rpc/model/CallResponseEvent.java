package com.chinaxing.framework.rpc.model;

/**
 * Created by LambdaCat on 15/8/22.
 */
public class CallResponseEvent {
    private int id = -1;
    private Object value;
    private String destination;
    private Throwable exception;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Throwable getException() {
        return exception;
    }

    public void setException(Throwable exception) {
        this.exception = exception;
    }
}
