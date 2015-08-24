package com.chinaxing.framework.rpc;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by LambdaCat on 15/8/21.
 */
public class RemoteCallPromise<V> implements Promise<V> {
    private volatile boolean isDone = false;
    private volatile boolean isCancelled = false;
    private volatile boolean isFailure = false;
    private volatile V data;
    private volatile String reason;
    private volatile Thread t;
    private CountDownLatch latch = new CountDownLatch(1);

    public void setSuccess(V v) {
        isDone = true;
        data = v;
        latch.countDown();
    }

    public void setFailure(String reason) {
        isFailure = true;
        this.reason = reason;
        latch.countDown();
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        if (isDone) return false;
        isCancelled = true;
        if (mayInterruptIfRunning) {
            if (t != null && t.isAlive()) {
                t.interrupt();
            }
        }
        return true;
    }

    public boolean isCancelled() {
        return isCancelled;
    }

    public boolean isDone() {
        return isDone;
    }

    public V get() throws InterruptedException, ExecutionException {
        if (isDone) return data;
        if (isFailure || isCancelled) return null;
        latch.await();
        if (isDone) return data;
        return null;
    }

    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (isDone) return data;
        if (isFailure || isCancelled) return null;
        latch.await(timeout, unit);
        if (isDone) return data;
        if (isCancelled || isFailure) return null;
        throw new TimeoutException(timeout + " " + unit);
    }
}
