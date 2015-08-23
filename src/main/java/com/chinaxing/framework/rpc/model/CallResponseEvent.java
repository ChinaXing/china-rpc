package com.chinaxing.framework.rpc.model;

/**
 * Created by LambdaCat on 15/8/22.
 */
public class CallResponseEvent {
    private int id;
    private Object value;

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
}
