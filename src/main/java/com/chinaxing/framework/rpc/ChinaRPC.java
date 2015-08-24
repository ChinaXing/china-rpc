package com.chinaxing.framework.rpc;

import com.chinaxing.framework.rpc.pipeline.CalleePipeline;
import com.chinaxing.framework.rpc.pipeline.CallerPipeline;
import com.chinaxing.framework.rpc.stub.CalleeStub;
import com.chinaxing.framework.rpc.stub.CallerStub;
import com.chinaxing.framework.rpc.stub.ServiceProvider;
import com.chinaxing.framework.rpc.transport.ConnectionManager;
import com.chinaxing.framework.rpc.transport.LoadBalance;
import com.chinaxing.framework.rpc.transport.RRLoadBalance;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

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
    private final Executor ioExecutor;
    private CalleeStub calleeStub;
    private CallerStub callerStub;
    private ServiceProvider provider;
    private String host;
    private int listen;
    private Executor callExecutor;
    private LoadBalance loadBalance;

    private ChinaRPC(ServiceProvider provider, long timeout, Executor ioExecutor,
                     Executor callExecutor, LoadBalance loadBalance,
                     String host, int listen) {
        this.provider = provider;
        this.timeout = timeout;
        this.callExecutor = callExecutor;
        this.ioExecutor = ioExecutor;
        this.loadBalance = loadBalance;
        this.listen = listen;
        this.host = host;
        ConnectionManager.setIoExecutor(ioExecutor);
    }

    private CallerStub callerStub() throws IOException {
        if (callerStub == null) {
            CallerPipeline callerPipeline = new CallerPipeline(callExecutor, 1024, loadBalance);
            callerStub = new CallerStub(provider, callerPipeline, timeout);
            callerPipeline.setStub(callerStub);
            callerPipeline.start();
        }

        return callerStub;
    }

    private CalleeStub calleeStub() throws IOException {
        if (calleeStub == null) {
            CalleePipeline calleePipeline = new CalleePipeline(callExecutor, ioExecutor, 1024, host, listen);
            calleeStub = new CalleeStub(calleePipeline);
            calleePipeline.setStub(calleeStub);
            calleePipeline.start();
        }
        return calleeStub;
    }

    public static class ChinaRPCBuilder {
        private ServiceProvider serviceProvider = new StaticServiceProvider();
        private long timeout = 5000;
        private int listen = 9119;
        private String host = "0.0.0.0";
        private LoadBalance loadBalance = new RRLoadBalance();
        private Executor ioExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2 - 1);
        private Executor callExecutor = Executors.newFixedThreadPool(128);

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

        public ChinaRPCBuilder setIoExecutor(Executor ioExecutor) {
            this.ioExecutor = ioExecutor;
            return this;
        }

        public ChinaRPCBuilder setCallExecutor(Executor callExecutor) {
            this.callExecutor = callExecutor;
            return this;
        }

        public ChinaRPC build() {
            return new ChinaRPC(serviceProvider, timeout, ioExecutor, callExecutor, loadBalance, host, listen);
        }

        public ChinaRPCBuilder setListen(int listen) {
            this.listen = listen;
            return this;
        }

        public ChinaRPCBuilder setHost(String host) {
            this.host = host;
            return this;
        }

        public ChinaRPCBuilder setLoadBalance(LoadBalance loadBalance) {
            this.loadBalance = loadBalance;
            return this;
        }
    }

    public static ChinaRPCBuilder getBuilder() {
        return new ChinaRPCBuilder();
    }

    public <T> T refer(Class<T> cls) throws IOException {
        return callerStub().refer(cls);

    }

    public <T> void export(T instance) throws IOException {
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
    public <T> T appointRefer(Class<T> cls, String address) throws IOException {
        return callerStub().refer(cls, address);
    }
}
