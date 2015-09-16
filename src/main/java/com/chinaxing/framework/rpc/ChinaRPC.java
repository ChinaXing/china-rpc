package com.chinaxing.framework.rpc;

import com.chinaxing.framework.rpc.exception.IllegalSettingsException;
import com.chinaxing.framework.rpc.model.WaitType;
import com.chinaxing.framework.rpc.pipeline.CalleePipeline;
import com.chinaxing.framework.rpc.pipeline.CallerPipeline;
import com.chinaxing.framework.rpc.stub.CalleeStub;
import com.chinaxing.framework.rpc.stub.CallerStub;
import com.chinaxing.framework.rpc.stub.ServiceProvider;
import com.chinaxing.framework.rpc.transport.IoEventLoopGroup;
import com.chinaxing.framework.rpc.transport.LoadBalance;
import com.chinaxing.framework.rpc.transport.RRLoadBalance;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

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
    private final int callExecutorCount;
    private final int ioEventLoopCount;
    private CalleeStub calleeStub;
    private CallerStub callerStub;
    private ServiceProvider provider;
    private String host;
    private int listen;
    private LoadBalance loadBalance;
    private WaitType waitType;

    private ChinaRPC(ServiceProvider provider, long timeout, int ioEventLoopCount,
                     int callExecutorCount, LoadBalance loadBalance,
                     String host, int listen, WaitType waitType) {
        this.provider = provider;
        this.timeout = timeout;
        this.loadBalance = loadBalance;
        this.listen = listen;
        this.host = host;
        this.ioEventLoopCount = ioEventLoopCount;
        this.waitType = waitType;
        this.callExecutorCount = callExecutorCount;
    }

    public static ChinaRPCBuilder getBuilder() {
        return new ChinaRPCBuilder();
    }

    private Executor buildExecutor(int count, final String name) {
        return Executors.newFixedThreadPool(count, new ThreadFactory() {
            private final AtomicInteger index = new AtomicInteger(0);

            public Thread newThread(Runnable r) {
                return new Thread(r, "ChinaRPC-" + name + "-" + index.getAndIncrement());
            }
        });
    }

    private CallerStub callerStub() throws IOException {
        if (callerStub == null) {
            IoEventLoopGroup ioEventLoopGroup = new IoEventLoopGroup(ioEventLoopCount, buildExecutor(ioEventLoopCount, "CallerIo"));
            CallerPipeline callerPipeline = new CallerPipeline(buildExecutor(4, "CallerExec"), waitType, ioEventLoopGroup, 1024, loadBalance);
            callerStub = new CallerStub(provider, callerPipeline, timeout);
            callerPipeline.setStub(callerStub);
            callerPipeline.start();
        }

        return callerStub;
    }

    private CalleeStub calleeStub() throws IOException {
        if (calleeStub == null) {
            IoEventLoopGroup ioEventLoopGroup = new IoEventLoopGroup(ioEventLoopCount, buildExecutor(ioEventLoopCount, "CalleeIo"));
            CalleePipeline calleePipeline = new CalleePipeline(buildExecutor(callExecutorCount, "CalleeExec"), waitType, callExecutorCount, ioEventLoopGroup, 1024, host, listen);
            calleeStub = new CalleeStub(calleePipeline);
            calleePipeline.setStub(calleeStub);
            calleePipeline.start();
        }
        return calleeStub;
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

    public static class ChinaRPCBuilder {
        private ServiceProvider serviceProvider = new StaticServiceProvider();
        private long timeout = 5000;
        private int listen = 9119;
        private String host = "0.0.0.0";
        private LoadBalance loadBalance = new RRLoadBalance();
        private int ioEventLoopCount = Runtime.getRuntime().availableProcessors() * 2 - 1;
        private int callExecutorCount = 8;
        private WaitType waitType = WaitType.LITE_BLOCK;

        private ChinaRPCBuilder() {
        }

        public WaitType getWaitType() {
            return waitType;
        }

        public ChinaRPCBuilder setWaitType(WaitType waitType) {
            this.waitType = waitType;
            return this;
        }

        public ChinaRPCBuilder setIoEventLoopCount(int ioEventLoopCount) {
            this.ioEventLoopCount = ioEventLoopCount;
            return this;
        }

        public ChinaRPCBuilder setTimeout(long timeout) {
            this.timeout = timeout;
            return this;
        }

        public ChinaRPCBuilder addProvider(String service, String address) {
            serviceProvider.provide(service, address);
            return this;
        }

        public ChinaRPC build() throws IllegalSettingsException {
            check();
            return new ChinaRPC(
                    serviceProvider,
                    timeout,
                    ioEventLoopCount,
                    callExecutorCount,
                    loadBalance,
                    host,
                    listen,
                    waitType
            );
        }

        private void check() throws IllegalSettingsException {
            if (callExecutorCount < 4) {
                throw new IllegalSettingsException("callExecutorCount", callExecutorCount, " must big than 4");
            }
            if (ioEventLoopCount < 1) {
                throw new IllegalSettingsException("ioEventLoopCount", ioEventLoopCount, " must > 1");
            }
            if (timeout < 0) {
                throw new IllegalSettingsException("timeout", timeout, " must > 0");
            }
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

        public int getCallExecutorCount() {
            return callExecutorCount;
        }

        public ChinaRPCBuilder setCallExecutorCount(int callExecutorCount) {
            this.callExecutorCount = callExecutorCount;
            return this;
        }
    }
}
