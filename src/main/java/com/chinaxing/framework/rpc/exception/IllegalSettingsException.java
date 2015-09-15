package com.chinaxing.framework.rpc.exception;

/**
 * Created by LambdaCat on 15/9/14.
 */
public class IllegalSettingsException extends Exception {
    private final String name;
    private final Object value;
    private final String message;

    public IllegalSettingsException(String name, Object value, String message) {
        super(name + "=" + value + " : " + message);
        this.name = name;
        this.value = value;
        this.message = message;
    }

    @Override
    public String toString() {
        return "IllegalSettings for : " + name + "value of : " + value + " ," + message;
    }
}
