package com.chinaxing.framework.rpc.model;

import java.nio.ByteBuffer;

/**
 * 二进制数据片段
 * Created by LambdaCat on 15/8/22.
 */
public class PacketEvent {
    private String destination;
    private ByteBuffer buffer;

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public void setBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }
}
