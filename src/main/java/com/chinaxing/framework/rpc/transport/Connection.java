package com.chinaxing.framework.rpc.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 一条连接
 * <p/>
 * Created by LambdaCat on 15/8/24.
 */
public class Connection {
    private static final Logger logger = LoggerFactory.getLogger(Connection.class);
    private String destination;
    private String host;
    private int port;
    private SocketChannel channel;
    private IOEventLoop ioEventLoop;
    private LinkedBlockingQueue<ByteBuffer> Q = new LinkedBlockingQueue<ByteBuffer>();
    private TransportHandler handler;
    private Executor executor;


    public Connection(String destination, Executor executor) throws IOException {
        this.destination = destination;
        this.executor = executor;
        String[] a = destination.split(":");
        this.host = a[0];
        this.port = Integer.valueOf(a[1]);
        start();

    }

    public Connection(String destination, SocketChannel channel, Executor ioExecutor) throws IOException {
        this.destination = destination;
        this.executor = ioExecutor;
        this.channel = channel;
        String[] a = destination.split(":");
        this.host = a[0];
        this.port = Integer.valueOf(a[1]);
        start();
    }

    public void start() throws IOException {
        if (channel == null)
            channel = SocketChannel.open(new InetSocketAddress(host, port));
        if (ioEventLoop == null) {
            ioEventLoop = new IOEventLoop();
        }
    }

    public void startEventLoop() throws IOException {
        if (!ioEventLoop.start) ioEventLoop.start();
    }

    public boolean isRunning() {
        return channel != null;
    }

    public TransportHandler getHandler() {
        return handler;
    }

    public void setHandler(TransportHandler handler) {
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

    public Executor getExecutor() {
        return executor;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public void close() throws IOException {
        this.ioEventLoop.close();
        Q.clear();
    }

    /**
     * 发送失败，上层从新选择Connection
     *
     * @param buffer
     * @throws IOException
     */
    public void send(ByteBuffer buffer) throws IOException {
        startEventLoop();
        if (!ioEventLoop.send(buffer)) {
            if (!ioEventLoop.start) {
                startEventLoop();
            }
            Q.add(buffer);
        }
    }


    /**
     * 每个SocketChannel 一个IOEventLoop
     */
    class IOEventLoop implements Runnable {
        private Selector selector;
        private volatile boolean start = false;
        private SelectionKey key;

        public synchronized void start() throws IOException {
            if (start) return;
            try {
                selector = Selector.open();
                if (channel == null)
                    channel = SocketChannel.open(new InetSocketAddress(host, port));
                channel.configureBlocking(false);
                key = channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                start = true;
                executor.execute(this);
            } catch (Throwable e) {
                logger.error("", e);
                if (selector.isOpen()) {
                    selector.close();
                }
                start = false;
            }
        }

        public boolean send(ByteBuffer buffer) throws IOException {
            if (key.isWritable()) {
                int c = channel.write(buffer);
                if (c < 0) {
                    close();
                    return false;
                }
                if (c == 0) return false;
                return true;
            }
            return false;
        }

        public void run() {
            try {
                while (start) {
                    int count = selector.select(1000);
                    if (count == 0) continue;
                    Set<SelectionKey> selected = selector.selectedKeys();
                    for (SelectionKey k : selected) {
                        if (k.isReadable()) {
                            SocketChannel channel = (SocketChannel) k.channel();
                            while (true) {
                                ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
                                int c = channel.read(buffer);
                                if (c < 0) {// channel is close by other side
                                    close();
                                    return;
                                }
                                if (c == 0) break;
                                buffer.flip();
                                handler.receive(destination, buffer);
                            }
                            continue;
                        }
                        if (k.isWritable()) {
                            SocketChannel channel = (SocketChannel) k.channel();
                            while (true) {
                                ByteBuffer buffer = Q.peek();
                                if (buffer != null) {
                                    int c = channel.write(buffer);
                                    if (c < 0) {
                                        close();
                                        return;
                                    }
                                    if (c == 0) break;
                                    Q.poll();
                                } else {
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (ClosedSelectorException e) {
                logger.error("Closed Selector", e);
            } catch (IOException e) {
                logger.error("", e);
                try {
                    close();
                } catch (Exception e2) {
                    logger.error("", e2);
                }
            }
        }

        public synchronized void close() throws IOException {
            start = false;
            channel.close();
            channel = null;
            key.cancel();
            selector.close();
        }
    }
}
