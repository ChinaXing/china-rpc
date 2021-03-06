package com.chinaxing.framework.rpc.transport;

import com.chinaxing.framework.rpc.protocol.SafeBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 一条连接
 * <p/>
 * Created by LambdaCat on 15/8/24.
 */
public class Connection {
    private static final Logger logger = LoggerFactory.getLogger(Connection.class);
    private final IOEventLoop ioEventLoop;
    private final ByteBuffer lBuf = ByteBuffer.allocate(16);
    private String destination;
    private String host;
    private int port;
    private SocketChannel channel = null;
    private LinkedBlockingQueue<ByteBuffer> Q = new LinkedBlockingQueue<ByteBuffer>();
    private ConnectionHandler handler;
    private volatile boolean start = false;
    private SafeBuffer buffer;
    private volatile int state;

    public Connection(String destination, ConnectionHandler handler, IOEventLoop ioEventLoop) {
        this.destination = destination;
        String[] a = destination.split(":");
        this.host = a[0];
        this.port = Integer.valueOf(a[1]);
        this.handler = handler;
        this.ioEventLoop = ioEventLoop;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public SafeBuffer getBuffer() {
        return buffer;
    }

    public void setBuffer(SafeBuffer buffer) {
        this.buffer = buffer;
    }

    public ByteBuffer getlBuf() {
        return lBuf;
    }

    public synchronized void start() throws Throwable {
        if (start) return;
        if (channel == null)
            channel = SocketChannel.open(new InetSocketAddress(host, port));
        if (!channel.isRegistered())
            ioEventLoop.register(this);
        start = true;
    }

    public boolean isRunning() {
        return start;
    }

    public ConnectionHandler getHandler() {
        return handler;
    }

    public void setHandler(ConnectionHandler handler) {
        this.handler = handler;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public void setChannel(SocketChannel channel) {
        this.channel = channel;
    }

    public synchronized void close() {
        start = false;
        if (channel != null) {
            try {
                ioEventLoop.cancel(channel);
            } catch (Throwable t) {
                logger.error("", t);
            }
            try {
                channel.close();
            } catch (Throwable e) {
                logger.error("", e);
            }
        }
        channel = null;
        Q.clear();
    }

    public synchronized void stop() {
        start = false;
    }

    public void send(SafeBuffer buffer) throws Throwable {
        Q.addAll(Arrays.asList(buffer.getBuffers()));
        ioEventLoop.wakeUpWrite(channel);
    }

    public void handle() {
        buffer.flip();
        buffer.getInt();
        handler.handle(destination, buffer);
    }

    public void pollData() {
        Q.poll();
    }

    public ByteBuffer peekData() {
        return Q.peek();
    }
}
