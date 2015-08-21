package com.chinaxing.framework.rpc;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by LambdaCat on 15/8/21.
 */
public interface Promise<V> extends Future<V> {
    void setSuccess(V v);

    void setFailure(String reason);
}
