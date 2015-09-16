package com.chinaxing.framework.rpc.transport;

import com.chinaxing.framework.rpc.protocol.SafeBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

/**
 * Created by LambdaCat on 15/9/12.
 */
public class IOEventLoop implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(IOEventLoop.class);
    private static final int READ_SIZE = 1;
    private static final int READ_DATA = 2;
    private Selector selector;
    private volatile boolean start = false;
    private Executor executor;
    private ConcurrentLinkedQueue<Connection> registerQ = new ConcurrentLinkedQueue<Connection>();

    public IOEventLoop(Executor executor) {
        this.executor = executor;
    }

    public synchronized void start() throws Throwable {
        if (start) return;
        start = true;
        if (selector == null || !selector.isOpen()) buildSelector();
        executor.execute(this);
    }

    private void buildSelector() throws Throwable {
        selector = Selector.open();
    }

    public synchronized void register(Connection connection) throws Throwable {
        connection.getChannel().configureBlocking(false);
        connection.getChannel().setOption(StandardSocketOptions.IP_TOS, 3);
        connection.getChannel().setOption(StandardSocketOptions.TCP_NODELAY, Boolean.TRUE);
        connection.setState(READ_SIZE);
        if (selector == null || !selector.isOpen()) buildSelector();
        registerQ.add(connection);
        if (!start) start();
        selector.wakeup();
    }


    /**
     * 注意NIO的线程安全性，NIO的register和select等操作需要在一个线程中进行，否则会block
     */
    private void doRegisterChannels() {
        while (true) {
            try {
                Connection connection = registerQ.poll();
                if (connection == null) return;
                connection.getChannel().register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, connection);
            } catch (ClosedChannelException e) {
                logger.error("Channel closed when we register it : {}", e);
            }
        }
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
                    int count = selector.select(1000);
                    doRegisterChannels();
                    if (count == 0) continue;
                    Iterator<SelectionKey> selected = selector.selectedKeys().iterator();
                    SELECT_KEY:
                    while (selected.hasNext()) {
                        SelectionKey k = selected.next();
                        selected.remove();
                        if (k.isReadable()) {
                            SocketChannel channel = (SocketChannel) k.channel();
                            Connection connection = (Connection) k.attachment();
                            if (connection == null) {
                                logger.error("Connection of channel : {} is NULL, ignore !", channel);
                                continue;
                            }
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
                                        if (c < 0) {
                                            connection.close();
                                            continue SELECT_KEY;
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
                            /**
                             * clear write writable listener
                             */
                            k.interestOps(k.interestOps() & ~SelectionKey.OP_WRITE);
                            SocketChannel channel = (SocketChannel) k.channel();
                            Connection connection = (Connection) k.attachment();
                            if (connection == null) {
                                logger.error("Connection of channel : {} is NULL, ignore !", channel);
                                continue;
                            }
                            while (true) {
                                ByteBuffer buffer = connection.peekData();
                                if (buffer != null) {
                                    try {
                                        int c = channel.write(buffer);
                                        if (c < 0) {
                                            connection.close();
                                            continue SELECT_KEY;
                                        }
                                        if (c == 0) {
                                            /**
                                             * not write over, listen writable again
                                             */
                                            k.interestOps(k.interestOps() & ~SelectionKey.OP_WRITE);
                                            break;
                                        }
                                        connection.pollData();
                                    } catch (NotYetConnectedException e) {
                                        connection.close();
                                        continue SELECT_KEY;
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
            for (SelectionKey k : selector.keys()) {
                k.cancel();
                ((Connection) k.attachment()).close();
            }
            if (selector.isOpen())
                selector.close();
        } catch (Throwable e2) {
            logger.error("", e2);
        } finally {
            selector = null;
        }
    }

    public void wakeUpWrite(SocketChannel channel) {
        SelectionKey k = channel.keyFor(selector);
        if (k != null) {
            if ((k.interestOps() & SelectionKey.OP_WRITE) == 0) {
                k.interestOps(k.interestOps() | SelectionKey.OP_WRITE);
            }
            selector.wakeup();
        }
    }
}
