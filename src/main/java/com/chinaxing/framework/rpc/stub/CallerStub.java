package com.chinaxing.framework.rpc.stub;

import com.chinaxing.framework.rpc.model.CallResponseEvent;

import java.util.List;

/**
 * Stub是代理层
 * <p/>
 * CallerStub 来代理调用方
 * <p/>
 * Created by LambdaCat on 15/8/21.
 */
public class CallerStub {
    /**
     * 生成服务对象引用
     * <p/>
     * 将会引用服务导出者导出的服务
     *
     * @param service 引用的服务类型，一个接口
     * @param <T>
     * @return 服务类型的一个实例
     */
    public <T> T refer(Class<T> service) {
        return null;
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
    public <T> T refer(Class<T> service, String address) {
        return null;
    }

    public <T> BroadCastReferWrapper<T> broadCastRefer(Class<T> service) {
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

    }
}
