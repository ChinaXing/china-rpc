package com.chinaxing.framework.rpc.transport;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 一组IO事件执行器
 * Created by LambdaCat on 15/9/12.
 */
public class IoEventLoopGroup {
    private IOEventLoop[] eventLoops;
    private final int size;
    private AtomicInteger index = new AtomicInteger(0);

    public IoEventLoopGroup(int size, Executor executor) {
        this.size = size;
        eventLoops = new IOEventLoop[size];
        for (int i = 0; i < size; i++) {
            eventLoops[i] = new IOEventLoop(executor);
        }
    }

    public IOEventLoop getIoEventLoop() throws Throwable {
        IOEventLoop loop = eventLoops[index.getAndIncrement() % size];
        loop.start();
        return loop;
    }
}
