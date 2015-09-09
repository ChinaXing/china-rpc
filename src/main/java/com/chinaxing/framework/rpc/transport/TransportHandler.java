package com.chinaxing.framework.rpc.transport;

import com.chinaxing.framework.rpc.model.EventContext;
import com.chinaxing.framework.rpc.model.PacketEvent;
import com.chinaxing.framework.rpc.pipeline.Pipeline;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 负责发送数据和接受数据
 * 采用NIO，Selector，服务端采用actor模型
 * 1. Acceptor 一个线程
 * 2. 每个connection 分配一个进程（connection不会太多，所以还好）
 * Created by LambdaCat on 15/8/21.
 */
public class TransportHandler {
    private static final Logger logger = LoggerFactory.getLogger(TransportHandler.class);
    private final Pipeline pipeline;
    private volatile boolean started = false;
    private LoadBalance loadBalance;

    public void send(PacketEvent packetEvent) {
        String dest = packetEvent.getDestination();
        while (true) {
            if (dest == null) {
                if (loadBalance == null) {
                    logger.error("destination is Null && loadBalance is Null");
                    EventContext<PacketEvent> ev = pipeline.up();
                    ev.getEvent().setException(new IllegalStateException("destination is Null && loadBalance is Null "));
                    pipeline.publish(ev);
                    return;
                }
                dest = loadBalance.select(packetEvent.getAvailableDestinations());
                if (dest == null) {
                    logger.error("cannot found destination from : {}", packetEvent.getAvailableDestinations());
                    EventContext<PacketEvent> ev = pipeline.up();
                    ev.getEvent().setException(new Exception("cannot found available destination from : "
                            + packetEvent.getAvailableDestinations()));
                    pipeline.publish(ev);
                    return;
                }
            }
            try {
                Connection connection = ConnectionManager.getConnection(dest);
                if (connection.getHandler() == null) {
                    connection.setHandler(this);
                }
                connection.send(packetEvent.getBuffer());
                return;
            } catch (Exception e) {
                logger.error("", e);
//                EventContext<PacketEvent> ev = pipeline.up();
//                ev.getEvent().setException(e);
//                pipeline.publish(ev);
            }
        }
    }

    public synchronized void startServer(Executor executor, String address, int port) throws IOException {
        if (started) return;
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open().bind(new InetSocketAddress(address, port));
        startServerEventLoop(executor, serverSocketChannel);
        started = true;
    }

    private void startServerEventLoop(Executor executor, final ServerSocketChannel serverSocketChannel) throws IOException {
        final Selector selector = Selector.open();
        serverSocketChannel.configureBlocking(false);
        final SelectionKey key = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        executor.execute(new Runnable() {
            public void run() {
                try {
                    while (true) {
                        int c = selector.select(500);
                        if (c == 0) continue;
                        Set<SelectionKey> ks = selector.selectedKeys();
                        for (SelectionKey k : ks) {
                            if (k.isAcceptable()) {
                                SocketChannel channel = serverSocketChannel.accept();
                                Connection connection = ConnectionManager.addConnection(channel);
                                connection.setHandler(TransportHandler.this);
                                connection.startEventLoop();
                            }
                        }
                    }
                } catch (Throwable e) {
                    logger.error("", e);
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

    public TransportHandler(Pipeline pipeline) {
        this.pipeline = pipeline;
    }

    private ConcurrentHashMap<String, LinkedBlockingQueue<ByteBuffer>> sendQueue =
            new ConcurrentHashMap<String, LinkedBlockingQueue<ByteBuffer>>();

    public void receive(String destination, ByteBuffer buffer) {
        EventContext<PacketEvent> ev = pipeline.up();
        ev.getEvent().setBuffer(buffer);
        ev.getEvent().setDestination(destination);
        pipeline.publish(ev);
    }
}
