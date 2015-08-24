package com.chinaxing.framework.rpc.pipeline;

import com.chinaxing.framework.rpc.model.EventContext;
import com.chinaxing.framework.rpc.model.PacketEvent;

/**
 * Created by LambdaCat on 15/8/24.
 */
public interface Pipeline<U, D> {
    void start() throws Exception;

    EventContext<U> up();

    EventContext<D> down();

    <E> void publish(EventContext<E> eventContext);
}
