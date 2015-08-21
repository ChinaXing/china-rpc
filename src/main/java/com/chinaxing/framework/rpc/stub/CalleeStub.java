package com.chinaxing.framework.rpc.stub;

import com.chinaxing.framework.rpc.ChinaRPC;
import com.chinaxing.framework.rpc.Promise;
import com.chinaxing.framework.rpc.RemoteCallPromise;
import com.chinaxing.framework.rpc.model.CallRequestEvent;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stub 是代理层
 * <p/>
 * Callee Stub负责代理被调用者
 * <p/>
 * Created by LambdaCat on 15/8/20.
 */
public class CalleeStub {
    /**
     * 导出服务
     *
     * @param clz      要导出的类型
     * @param instance 服务器实现对象
     * @param <T>
     */
    public <T> void export(Class<T> clz, T instance) {

    }

    /**
     * 导出服务
     * 导出所有 instance实现的接口为服务，实现对象为instance
     *
     * @param instance
     * @param <T>
     */
    public <T> void export(T instance) {

    }

    public void call(CallRequestEvent event) {

    }
}
