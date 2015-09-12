package com.chinaxing.framework.rpc.transport;

import com.chinaxing.framework.rpc.protocol.SafeBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.io.ByteToCharUnicodeBigUnmarked;

import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Created by LambdaCat on 15/9/12.
 */
public class IOEventLoop implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(IOEventLoop.class);
    private Selector selector;
    private volatile boolean start = false;
    private Executor executor;
    private static final int READ_SIZE = 1;
    private static final int READ_DATA = 2;

    public IOEventLoop(Executor executor) {
        this.executor = executor;
    }

    public synchronized void start() throws Throwable {
        if (start) return;
        selector = Selector.open();
        start = true;
        executor.execute(this);
    }

    public synchronized void register(Connection connection) throws Throwable {
        if (!start) start();
        connection.getChannel().configureBlocking(false);
        connection.setState(READ_SIZE);
        SelectionKey k = connection.getChannel().register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        k.attach(connection);
    }

    public void cancel(SocketChannel channel) throws Throwable {
        if (channel.isRegistered()) {
            SelectionKey k = channel.keyFor(selector);
            if (k != null) {
                k.cancel();
            }
        }
    }

    public void run() {
        try {
            while (start) {
                try {
                    selector.select(500);
                    Set<SelectionKey> selected = selector.selectedKeys();
                    SELECT_KEY:
                    for (SelectionKey k : selected) {
                        if (k.isReadable()) {
                            SocketChannel channel = (SocketChannel) k.channel();
                            Connection connection = (Connection) k.attachment();
                            DO_READ:
                            while (true) {
                                switch (connection.getState()) {
                                    case READ_SIZE: {
                                        ByteBuffer lBuf = connection.getlBuf();
                                        int c = channel.read(lBuf);
                                        if (c < 0) {// channel is close by other side
                                            connection.close();
                                            continue SELECT_KEY;
                                        }
                                        if (c == 0) break DO_READ;
                                        ByteBuffer dBuf = lBuf.duplicate();
                                        dBuf.flip();
                                        try {
                                            int size = dBuf.getInt();

                                            SafeBuffer buffer = new SafeBuffer(size);
                                            connection.setBuffer(buffer);

                                            lBuf.position(dBuf.position());
                                            int limit = lBuf.limit();
                                            lBuf.limit(lBuf.position() + Math.min(buffer.remaining(), dBuf.remaining()));
                                            buffer.put(lBuf);
                                            lBuf.limit(limit);
                                            lBuf.compact();
                                            /**
                                             * 数据接收完成
                                             */
                                            if (!buffer.hasRemaining()) {
                                                connection.handle();
                                                connection.setState(READ_SIZE);
                                                break;
                                            }
                                            connection.setState(READ_DATA);
                                            break;
                                        } catch (IndexOutOfBoundsException e) {
                                            break DO_READ;
                                        }
                                    }
                                    case READ_DATA: {
                                        SafeBuffer buffer = connection.getBuffer();
                                        int c = channel.read(buffer.getWrite());
                                        if (c < 0) {// channel is close by other side
                                            return;
                                        }
                                        if (c == 0) break DO_READ;
                                        /**
                                         * 数据接收完成
                                         */
                                        if (!buffer.hasRemaining()) {
                                            connection.handle();
                                            connection.setState(READ_SIZE);
                                            break;
                                        }

                                        /**
                                         * 继续接收数据
                                         */
                                    }
                                }
                            }
                        }
                        if (k.isWritable()) {
                            SocketChannel channel = (SocketChannel) k.channel();
                            Connection connection = (Connection) k.attachment();
                            while (true) {
                                ByteBuffer buffer = connection.peekData();
                                if (buffer != null) {
                                    try {
                                        int c = channel.write(buffer);
                                        if (c < 0) {
                                            cancel(connection.getChannel());
                                            connection.close();
                                            break;
                                        }
                                        if (c == 0) break;
                                        connection.pollData();
                                    } catch (NotYetConnectedException e) {
                                        connection.close();
                                        break;
                                    }
                                } else {
                                    break;
                                }
                            }
                        }
                    }
                } catch (ClosedChannelException e) {
                    logger.error("e", e);
                }
            }
        } catch (Throwable t) {
            logger.error("exception", t);
        } finally {
            close();
        }
    }

    public synchronized void close() {
        try {
            start = false;
            if (selector.isOpen())
                selector.close();
            for (SelectionKey k : selector.keys()) {
                k.cancel();
                ((Connection) k.attachment()).stop();
            }
            selector = null;
        } catch (Throwable e2) {
            logger.error("", e2);
        }
    }

    public void wakeup() {
        selector.wakeup();
    }
}
