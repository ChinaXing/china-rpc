package com.chinaxing.framework.rpc.stub;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by LambdaCat on 15/8/21.
 */
public class BroadCastReferWrapper<T> {
    private CallerStub stub;
    private Class<T> clz;

    public BroadCastReferWrapper(CallerStub stub, Class<T> clz) {
        this.stub = stub;
        this.clz = clz;
    }

    public List<Object> call(Method method, Object[] args) {
        return stub.broadCastCall(clz, method.getName(), args);
    }
}
