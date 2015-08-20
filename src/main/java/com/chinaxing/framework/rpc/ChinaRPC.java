package com.chinaxing.framework.rpc;

import com.chinaxing.framework.rpc.exception.UnsupportedArgumentType;
import com.chinaxing.framework.rpc.stub.Provider;
import com.chinaxing.framework.rpc.stub.ServiceProvider;
import com.chinaxing.framework.rpc.stub.Stub;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * RCP框架的工厂类
 * <p/>
 * 1. 生成rpc调用方的代理 refer
 * 2. 生成rpc被调用方的导出 exporter
 * <p/>
 * 自动启动RCP代理后台服务
 * <p/>
 * Created by LambdaCat on 15/8/20.
 */
public class ChinaRPC {
    private Stub stub;

    private ChinaRPC(ServiceProvider provider, long timeout) {
        stub = new Stub(provider, timeout);
    }

    static class ChinaRPCBuilder {
        private ServiceProvider serviceProvider;
        private long timeout;

        public ChinaRPCBuilder setTimeout(long timeout) {
            this.timeout = timeout;
            return this;
        }

        public ChinaRPCBuilder setProviders(List<String> providers) {
            serviceProvider = new StaticServiceProvider(providers);
            return this;
        }

        public ChinaRPC build() {
            return new ChinaRPC(serviceProvider, timeout);
        }

    }

    public <T> T refer(Class<T> cls) throws UnsupportedArgumentType {
        return stub.refer(cls);

    }

    public <T> boolean export(T instance) {
        return stub.export(instance);
    }
}
