package com.chinaxing.framework.rpc.transport;

import com.chinaxing.framework.rpc.model.PacketEvent;
import com.lmax.disruptor.RingBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * IO 事件循环
 * Created by LambdaCat on 15/8/24.
 */
public class IOEventLoop implements Runnable {
    private Selector selector;
    private Set<SelectionKey> selectionKeys;
    private RingBuffer<PacketEvent> sendRingBuffer;
    private RingBuffer<PacketEvent> receiveRingBuffer;
    private ConcurrentHashMap<String, LinkedBlockingQueue<ByteBuffer>> sendQueue = new ConcurrentHashMap<String, LinkedBlockingQueue<ByteBuffer>>();

    public IOEventLoop(RingBuffer<PacketEvent> sendRingBuffer, RingBuffer<PacketEvent> receiveRingBuffer) {
        this.selector = selector;
        this.receiveRingBuffer = receiveRingBuffer;
        this.sendRingBuffer = sendRingBuffer;
    }

    public void open() throws IOException {
        selector = Selector.open();
        selectionKeys = selector.selectedKeys();
    }

    public void register(SocketChannel channel) throws IOException {
        channel.configureBlocking(false);
        SelectionKey key = channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        String address = channel.getRemoteAddress().toString();
        String dest = address.substring(0, address.indexOf('/'));
        key.attach(dest);
    }

    public void registerServer(ServerSocketChannel channel) throws IOException {
        channel.configureBlocking(false);
        SelectionKey key = channel.register(selector, SelectionKey.OP_ACCEPT);
    }

    public void write(PacketEvent packetEvent) {
        String dest = packetEvent.getDestination();
        LinkedBlockingQueue<ByteBuffer> Q = sendQueue.get(dest);
        if (Q == null) {
            Q = new LinkedBlockingQueue<ByteBuffer>();
            LinkedBlockingQueue<ByteBuffer> QOld = sendQueue.putIfAbsent(dest, Q);
            if (QOld != null) Q = QOld;
        }
        Q.add(packetEvent.getBuffer());
    }


    public void run() {
        while (true) {
            try {
                int count = selector.select(5000);
                if (count == 0) continue;
                Set<SelectionKey> selected = selector.selectedKeys();
                for (SelectionKey k : selected) {
                    if (k.isAcceptable()) {
                        ServerSocketChannel channel = (ServerSocketChannel) k.channel();
                        SocketChannel channel0 = channel.accept();
                        register(channel0);
                        continue;
                    }
                    if (k.isReadable()) {
                        SocketChannel channel = (SocketChannel) k.channel();
                        ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
                        channel.read(buffer);
                        long seq = receiveRingBuffer.next();
                        PacketEvent ev = receiveRingBuffer.get(seq);
                        ev.setBuffer(buffer);
                        receiveRingBuffer.publish(seq);
                        continue;
                    }
                    if (k.isWritable()) {
                        SocketChannel channel = (SocketChannel) k.channel();
                        LinkedBlockingQueue<ByteBuffer> Q = sendQueue.get(k.attachment());
                        if (Q == null) continue;
                        ByteBuffer buffer = Q.poll();
                        if (buffer != null) {
                            channel.write(buffer);
                        }
                    }
                }
            } catch (ClosedChannelException e) {

            } catch (IOException e) {

            }
        }
    }
}
