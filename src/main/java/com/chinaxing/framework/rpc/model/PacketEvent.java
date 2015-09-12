package com.chinaxing.framework.rpc.model;

import com.chinaxing.framework.rpc.protocol.SafeBuffer;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * 二进制数据片段
 * Created by LambdaCat on 15/8/22.
 */
public class PacketEvent {
    private String destination;
    private List<String> availableDestinations;
    private Throwable exception;
    private SafeBuffer buffer;
    private int id = -1;

    public void setId(int id) {
        this.id = id;
    }

    public List<String> getAvailableDestinations() {
        return availableDestinations;
    }

    public void setAvailableDestinations(List<String> availableDestinations) {
        this.availableDestinations = availableDestinations;
    }

    public Throwable getException() {
        return exception;
    }

    public void setException(Throwable exception) {
        this.exception = exception;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public SafeBuffer getBuffer() {
        return buffer;
    }

    public void setBuffer(SafeBuffer buffer) {
        this.buffer = buffer;
    }

    public int getId() {
        return id;
    }
}
