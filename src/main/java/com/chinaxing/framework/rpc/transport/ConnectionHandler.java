package com.chinaxing.framework.rpc.transport;

import com.chinaxing.framework.rpc.protocol.SafeBuffer;

/**
 * Created by LambdaCat on 15/9/12.
 */
public interface ConnectionHandler {
    void handle(String destination, SafeBuffer buffer);
}
