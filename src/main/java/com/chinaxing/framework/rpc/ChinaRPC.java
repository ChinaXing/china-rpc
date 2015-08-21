package com.chinaxing.framework.rpc;

import com.chinaxing.framework.rpc.model.ServiceInfo;
import com.chinaxing.framework.rpc.stub.CalleeStub;
import com.chinaxing.framework.rpc.stub.CallerStub;
import com.chinaxing.framework.rpc.stub.ServiceProvider;

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
    private final long timeout;
    private CalleeStub calleeStub;
    private CallerStub callerStub;
    private ServiceProvider provider;
    private int listen;

    private ChinaRPC(ServiceProvider provider, long timeout) {
        this.provider = provider;
        this.timeout = timeout;
    }

    private CallerStub callerStub() {
        if (callerStub == null) {
            callerStub = new CallerStub();
        }

        return callerStub;
    }

    private CalleeStub calleeStub() {
        if (calleeStub == null) {
            calleeStub = new CalleeStub();
        }
        return calleeStub;
    }

    public static class ChinaRPCBuilder {
        private ServiceProvider serviceProvider = new StaticServiceProvider();
        private long timeout = 5000;
        private int listen = 9119;

        private ChinaRPCBuilder() {
        }

        public ChinaRPCBuilder setTimeout(long timeout) {
            this.timeout = timeout;
            return this;
        }

        public ChinaRPCBuilder addProvider(String service, String address) {
            serviceProvider.provide(service, address);
            return this;
        }

        public ChinaRPC build() {
            return new ChinaRPC(serviceProvider, timeout);
        }

        public ChinaRPCBuilder setListen(int listen) {
            this.listen = listen;
            return this;
        }
    }

    public static ChinaRPCBuilder getBuilder() {
        return new ChinaRPCBuilder();
    }

    public <T> T refer(Class<T> cls) {
        return callerStub().refer(cls);

    }

    public <T> void export(T instance) {
        calleeStub().export(instance);
    }

    /**
     * 指定地址的调用
     *
     * @param cls
     * @param address
     * @param <T>
     * @return
     */
    public <T> T appointRefer(Class<T> cls, String address) {
        return callerStub().refer(cls, address);
    }
}
