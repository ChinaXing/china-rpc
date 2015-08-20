package com.chinaxing.framework.rpc.stub;

import com.chinaxing.framework.rpc.ChinaRPC;
import com.chinaxing.framework.rpc.exception.UnsupportedArgumentType;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Created by LambdaCat on 15/8/20.
 */
public class Stub {
    public Stub(ServiceProvider provider, long timeout) {

    }

    public <T> boolean export(T instance) {
        return true;
    }

    public <T> T refer(Class<T> cls) throws UnsupportedArgumentType {
        if (cls.isInterface()) {
            return (T) Proxy.newProxyInstance(ChinaRPC.class.getClassLoader(), new Class[]{cls}, new InvocationHandler() {
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    return "hello,world";
                }
            });
        } else {
            throw new UnsupportedArgumentType("refered class must be an Interface, but found class : " + cls.getClass().getName());
        }
    }
}
