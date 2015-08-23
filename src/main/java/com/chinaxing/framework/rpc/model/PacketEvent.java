package com.chinaxing.framework.rpc.model;

import java.nio.ByteBuffer;

/**
 * 二进制数据片段
 * Created by LambdaCat on 15/8/22.
 */
public class PacketEvent {
    private ByteBuffer buffer;

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public void setBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }
}
