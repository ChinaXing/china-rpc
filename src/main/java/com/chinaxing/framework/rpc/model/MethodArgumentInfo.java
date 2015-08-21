package com.chinaxing.framework.rpc.model;

/**
 * Created by LambdaCat on 15/8/21.
 */
public class MethodArgumentInfo {
    String clzName;
    Object value;

    @Override
    public String toString() {
        return "MethodArgumentInfo{" +
                "clzName='" + clzName + '\'' +
                ", value=" + value +
                '}';
    }
}
