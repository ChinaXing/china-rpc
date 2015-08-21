package com.chinaxing.framework.rpc;

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

    public void setSuccess(V v) {
        isDone = true;
        data = v;
        synchronized (this) {
            notifyAll();
        }
    }

    public void setFailure(String reason) {
        isFailure = true;
        this.reason = reason;
        synchronized (this) {
            notifyAll();
        }
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
        synchronized (this) {
            wait();
        }
        if (isDone) return data;
        return null;
    }

    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (isDone) return data;
        if (isFailure || isCancelled) return null;
        synchronized (this) {
            wait(timeout);
        }
        if (isDone) return data;
        if (isCancelled || isFailure) return null;
        throw new TimeoutException();
    }
}
