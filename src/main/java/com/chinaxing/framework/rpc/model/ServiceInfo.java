package com.chinaxing.framework.rpc.model;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 描述一个服务的类
 * 一个服务包含：类，方法名，方法参数
 * Created by LambdaCat on 15/8/21.
 */
public class ServiceInfo {
    String clzName;
    String methodName;
    List<MethodArgumentInfo> argumentInfoList;

    @Override
    public String toString() {
        return "ServiceInfo{" +
                "clzName='" + clzName + '\'' +
                ", methodName='" + methodName + '\'' +
                ", argumentInfoList=" + argumentInfoList +
                '}';
    }

    public ServiceInfo(String clzName, String methodName, List<MethodArgumentInfo> argumentInfoList) {
        this.clzName = clzName;
        this.methodName = methodName;
        this.argumentInfoList = argumentInfoList;
    }
}
