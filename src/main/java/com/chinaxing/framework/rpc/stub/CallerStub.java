package com.chinaxing.framework.rpc.stub;

import com.chinaxing.framework.rpc.RemoteCallPromise;
import com.chinaxing.framework.rpc.exception.CallException;
import com.chinaxing.framework.rpc.model.CallRequestEvent;
import com.chinaxing.framework.rpc.model.CallResponseEvent;
import com.chinaxing.framework.rpc.model.EventContext;
import com.chinaxing.framework.rpc.pipeline.CallerPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stub是代理层
 * <p/>
 * CallerStub 来代理调用方
 * <p/>
 * Created by LambdaCat on 15/8/21.
 */
public class CallerStub {
    private static final Logger logger = LoggerFactory.getLogger(CallerStub.class);
    private final AtomicInteger index = new AtomicInteger(0);
    private final Map<Class, Object> proxyCache = new HashMap<Class, Object>();
    private final Map<String, Object> uniqueProxyCache = new HashMap<String, Object>();
    private final ConcurrentHashMap<Integer, RemoteCallPromise> promiseMap = new ConcurrentHashMap<Integer, RemoteCallPromise>();
    private ServiceProvider serviceProvider;
    private CallerPipeline callerPipeline;
    private long timeout = 3000;

    public CallerStub(ServiceProvider serviceProvider, CallerPipeline callerPipeline) {
        this.callerPipeline = callerPipeline;
        this.serviceProvider = serviceProvider;
    }

    public CallerStub(ServiceProvider serviceProvider, CallerPipeline callerPipeline, long timeout) {
        this.serviceProvider = serviceProvider;
        this.callerPipeline = callerPipeline;
        this.timeout = timeout;
    }

    /**
     * 生成服务对象引用
     * <p/>
     * 将会引用服务导出者导出的服务
     *
     * @param service 引用的服务类型，一个接口
     * @param <T>
     * @return 服务类型的一个实例
     */
    public synchronized <T> T refer(final Class<T> service) {
        T proxy = (T) proxyCache.get(service);
        if (proxy != null) return proxy;
        proxy = buildProxy(service, null);
        proxyCache.put(service, proxy);
        return proxy;
    }

    /**
     * 生成服务对象引用，明确指定被引用方的地址
     * <p/>
     * 用来支持指定调用目标的RPC
     *
     * @param service 引用服务的类型，一个接口
     * @param address 引用服务提供者的地址
     * @param <T>
     * @return
     */
    public synchronized <T> T refer(final Class<T> service, final String address) {
        @SuppressWarnings("unchecked")
        T proxy = (T) uniqueProxyCache.get(service.getName() + "#" + address);
        if (proxy != null) return proxy;
        proxy = buildProxy(service, address);
        uniqueProxyCache.put(service.getName() + "#" + address, proxy);
        return proxy;
    }

    @SuppressWarnings("unchecked")
    private <T> T buildProxy(final Class<T> service, final String address) {
        return (T) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{service}, new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                EventContext<CallRequestEvent> ev = callerPipeline.down();
                CallRequestEvent ce = ev.getEvent();
                ce.setClz(service);
                ce.setMethod(method);
                ce.setArguments(args == null ? new Object[]{} : args);
                int idx = index.getAndIncrement();
                ce.setId(idx);
                if (address == null) {
                    ce.setAvailableDestinations(serviceProvider.getProvider(service.getName()));
                } else {
                    ce.setDestination(address);
                }
                RemoteCallPromise promise = new RemoteCallPromise();
                promiseMap.put(idx, promise);
                callerPipeline.publish(ev);
                Object r = promise.get(timeout, TimeUnit.MILLISECONDS);
                promiseMap.remove(idx);
                if (promise.isCancelled()) {
                    throw new CancellationException("method : " + method + ",idx : " + idx);
                }
                if (promise.isFailure()) {
                    if (promise.isException())
                        throw new CallException("CallException", promise.getCause());
                    else
                        throw new CallException(promise.getReason());
                }
                return r;
            }
        });
    }

    public synchronized <T> BroadCastReferWrapper<T> broadCastRefer(Class<T> service) {
        return new BroadCastReferWrapper<T>(this, service);
    }

    /**
     * 执行广播调用
     *
     * @param clz
     * @param name
     * @param args
     * @return
     */
    public <T> List<Object> broadCastCall(Class<T> clz, String name, Object[] args) {
        return null;
    }

    public void response(CallResponseEvent event) {
        RemoteCallPromise promise = promiseMap.get(event.getId());
        if (promise == null) {
            logger.warn("Cannot found promise Of : {}, maybe time-outed/canceled/interrupted", event.getId());
            return;
        }
        if (event.getException() != null) {
            promise.setException(event.getException());
            return;
        }
        promise.setSuccess(event.getValue());
    }
}
