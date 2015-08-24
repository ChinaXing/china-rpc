package com.chinaxing.framework.rpc.transport;

import java.nio.ByteBuffer;

/**
 * Created by LambdaCat on 15/8/24.
 */
public interface EventListener {
    ByteBuffer toWrite();

    void read(ByteBuffer buffer);

    void next();
}
