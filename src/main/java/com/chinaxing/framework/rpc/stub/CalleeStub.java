package com.chinaxing.framework.rpc.stub;

import com.chinaxing.framework.rpc.ChinaRPC;
import com.chinaxing.framework.rpc.Promise;
import com.chinaxing.framework.rpc.RemoteCallPromise;
import com.chinaxing.framework.rpc.model.CallRequestEvent;
import com.chinaxing.framework.rpc.model.CallResponseEvent;
import com.chinaxing.framework.rpc.model.EventContext;
import com.chinaxing.framework.rpc.pipeline.CalleePipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
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
    private static final Logger logger = LoggerFactory.getLogger(CalleeStub.class);
    private final ConcurrentHashMap<Class, Object> exportMap = new ConcurrentHashMap<Class, Object>();
    private final CalleePipeline pipeline;

    public CalleeStub(CalleePipeline pipeline) {
        this.pipeline = pipeline;
    }

    /**
     * 导出服务
     *
     * @param clz      要导出的类型
     * @param instance 服务器实现对象
     * @param <T>
     */
    public <T> void export(Class<T> clz, T instance) {
        exportMap.put(clz, instance);
    }

    /**
     * 导出服务
     * 导出所有 instance实现的接口为服务，实现对象为instance
     *
     * @param instance
     * @param <T>
     */
    public <T> void export(T instance) {
        for (Class c : instance.getClass().getInterfaces())
            exportMap.put(c, instance);
    }

    public void call(CallRequestEvent event) {
        if (event.getException() != null) {
            logger.error("call exception : ", event.getException());
            return;
        }
        Class clz = event.getClz();
        if (clz == null) {
            logger.error("Clz is NULL :{}", event);
            return;
        }
        Object invoked = exportMap.get(clz);

        if (invoked == null) {
            Exception e = new Exception("Remote Service Not Found ! :" + clz.getName());
            doPublish(event.getDestination(), event.getId(), null, e);
            return;
        }
        Method m = event.getMethod();
        Object[] argument = event.getArguments();
        try {
            Object result = m.invoke(invoked, argument);
            doPublish(event.getDestination(), event.getId(), result, null);
        } catch (InvocationTargetException ex) {
            doPublish(event.getDestination(), event.getId(), null, ex.getTargetException());
        } catch (Throwable x) {
            doPublish(event.getDestination(), event.getId(), null, x);
        }
    }

    private void doPublish(String destination, int id, Object value, Throwable t) {
        EventContext<CallResponseEvent> ev = pipeline.down();
        CallResponseEvent e = ev.getEvent();
        e.setDestination(destination);
        e.setId(id);
        e.setValue(value);
        e.setException(t);
        pipeline.publish(ev);
    }
}
