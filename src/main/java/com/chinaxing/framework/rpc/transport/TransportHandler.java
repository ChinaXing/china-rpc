package com.chinaxing.framework.rpc.transport;

import com.chinaxing.framework.rpc.model.EventContext;
import com.chinaxing.framework.rpc.model.PacketEvent;
import com.chinaxing.framework.rpc.pipeline.Pipeline;
import com.chinaxing.framework.rpc.protocol.SafeBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 非单例，每一个PipeLine一个
 * 负责发送数据和接受数据
 * 采用NIO，Selector，服务端采用actor模型
 * 1. Acceptor 一个线程
 * 2. 每个connection 分配一个进程（connection不会太多，所以还好）
 * <p/>
 * todo 增加发送重试
 * Created by LambdaCat on 15/8/21.
 */
public class TransportHandler {
    private static final Logger logger = LoggerFactory.getLogger(TransportHandler.class);
    private final Pipeline pipeline;
    private volatile boolean started = false;
    private LoadBalance loadBalance;
    private ConnectionManager connectionManager;
    private IoEventLoopGroup ioEventLoopGroup;

    public TransportHandler(Pipeline pipeline, IoEventLoopGroup ioEventLoopGroup, LoadBalance loadBalance) {
        this.pipeline = pipeline;
        this.connectionManager = new ConnectionManager(ioEventLoopGroup, new ConnectionHandler() {
            public void handle(String destination, SafeBuffer buffer) {
                TransportHandler.this.receive(destination, buffer);
            }
        });
        this.loadBalance = loadBalance;
        if (loadBalance != null) loadBalance.setConnectionManager(connectionManager);
    }

    public void send(PacketEvent packetEvent) throws Throwable {
        String dest = packetEvent.getDestination();
        if (dest == null) {
            if (loadBalance == null) {
                logger.error("destination is Null && loadBalance is Null");
                throw new IllegalStateException("destination is Null && loadBalance is Null ");
            }
            dest = loadBalance.select(packetEvent.getAvailableDestinations());
            if (dest == null) {
                logger.error("cannot found destination from : {}", packetEvent.getAvailableDestinations());
                throw new Exception("cannot found available destination from : "
                        + packetEvent.getAvailableDestinations());
            }
        }
        Connection connection = null;
        try {
            connection = connectionManager.getConnection(dest);
            if (!connection.isRunning()) connection.start();
            connection.send(packetEvent.getBuffer());
        } catch (Throwable e) {
            if (connection != null) connection.close();
            throw e;
        }
    }

    public synchronized void startServer(String address, int port) throws IOException {
        if (started) return;
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open().bind(new InetSocketAddress(address, port));
        startServerEventLoop(serverSocketChannel);
        started = true;
    }

    private void startServerEventLoop(final ServerSocketChannel serverSocketChannel) throws IOException {
        final Selector selector = Selector.open();
        serverSocketChannel.configureBlocking(false);
        final SelectionKey key = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        Executors.newSingleThreadExecutor(new ThreadFactory() {
            private final AtomicInteger index = new AtomicInteger();

            public Thread newThread(Runnable r) {
                return new Thread(r, "ChinaRPC-server-io-thread-" + index.getAndIncrement());
            }
        }).execute(new Runnable() {
            public void run() {
                try {
                    while (true) {
                        /**
                         * server socket select will return become ready count, always 0
                         *
                         * so should not to check return count to determinate ready state.
                         */
                        selector.select();
                        Set<SelectionKey> ks = selector.selectedKeys();
                        for (SelectionKey k : ks) {
                            if (k.isAcceptable()) {
                                SocketChannel channel = serverSocketChannel.accept();
                                Connection connection = connectionManager.addConnection(channel);
                                connection.start();
                            }
                        }
                    }
                } catch (Throwable e) {
                    logger.error("close with cause : ", e);
                } finally {
                    key.cancel();
                    try {
                        serverSocketChannel.close();
                        selector.close();
                    } catch (Exception e1) {
                        logger.error("", e1);
                    }
                }
            }
        });
    }

    public synchronized void startClient(LoadBalance loadBalance) throws IOException {
        if (started) return;
        this.loadBalance = loadBalance;
        started = true;
    }

    private ConcurrentHashMap<String, LinkedBlockingQueue<ByteBuffer>> sendQueue =
            new ConcurrentHashMap<String, LinkedBlockingQueue<ByteBuffer>>();

    public void receive(String destination, SafeBuffer buffer) {
        try {
            EventContext<PacketEvent> ev = pipeline.up();
            ev.getEvent().setBuffer(buffer);
            ev.getEvent().setDestination(destination);
            pipeline.publish(ev);
        } catch (Throwable e) {
            logger.error("", e);
        }
    }
}
