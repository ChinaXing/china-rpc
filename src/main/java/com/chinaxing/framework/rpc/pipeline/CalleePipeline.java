package com.chinaxing.framework.rpc.pipeline;

import com.chinaxing.framework.rpc.model.CallRequestEvent;
import com.chinaxing.framework.rpc.model.CallResponseEvent;
import com.chinaxing.framework.rpc.model.EventContext;
import com.chinaxing.framework.rpc.model.PacketEvent;
import com.chinaxing.framework.rpc.protocol.ProtocolHandler;
import com.chinaxing.framework.rpc.stub.CalleeStub;
import com.chinaxing.framework.rpc.transport.ConnectionManager;
import com.chinaxing.framework.rpc.transport.TransportHandler;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Executor;

/**
 * 被调用方的处理链条
 * <p/>
 * DownStream : Stub ---(CallResponseEvent)---> Protocol ---(PacketEvent)---> Transport
 * <p/>
 * UpStream : Transport ---(PacketEvent)---> Protocol ---(CallRequestEvent)---> Stub
 * <p/>
 * Created by LambdaCat on 15/8/21.
 */
public class CalleePipeline implements Pipeline<PacketEvent, CallResponseEvent> {
    private static final Logger logger = LoggerFactory.getLogger(CalleePipeline.class);
    private final Executor executor;
    private final Executor ioExecutor;
    private final int capacity;
    private final int port;
    private final String host;
    private Disruptor<CallRequestEvent> callRequestEventDisruptor;
    private Disruptor<PacketEvent> downStreamPacketEventDisruptor;
    private Disruptor<PacketEvent> upStreamPacketEventDisruptor;
    private Disruptor<CallResponseEvent> callResponseEventDisruptor;
    private ProtocolHandler protocolHandler = new ProtocolHandler();
    private TransportHandler transportHandler = new TransportHandler(this);
    private CalleeStub stub;
    private RingBuffer<CallRequestEvent> callRequestEventRingBuffer;
    private RingBuffer<PacketEvent> downStreamPacketEventRingBuffer;
    private RingBuffer<PacketEvent> upStreamPacketEventRingBuffer;
    private RingBuffer<CallResponseEvent> callResponseEventRingBuffer;


    public CalleePipeline(Executor executor, Executor ioExecutor, int capacity, String host, int port) {
        this.executor = executor;
        this.capacity = capacity;
        this.ioExecutor = ioExecutor;
        this.host = host;
        this.port = port;

        init();
    }

    public void setStub(CalleeStub stub) {
        this.stub = stub;
    }

    private void init() {
        callRequestEventDisruptor = new Disruptor<CallRequestEvent>(new EventFactory<CallRequestEvent>() {
            public CallRequestEvent newInstance() {
                return new CallRequestEvent();
            }
        }, capacity, executor);

        downStreamPacketEventDisruptor = new Disruptor<PacketEvent>(new EventFactory<PacketEvent>() {
            public PacketEvent newInstance() {
                return new PacketEvent();
            }
        }, capacity, executor);

        upStreamPacketEventDisruptor = new Disruptor<PacketEvent>(new EventFactory<PacketEvent>() {
            public PacketEvent newInstance() {
                return new PacketEvent();
            }
        }, capacity, executor);

        callResponseEventDisruptor = new Disruptor<CallResponseEvent>(new EventFactory<CallResponseEvent>() {
            public CallResponseEvent newInstance() {
                return new CallResponseEvent();
            }
        }, capacity, executor);


        // chain together

        callRequestEventDisruptor.handleEventsWith(new EventHandler<CallRequestEvent>() {
            public void onEvent(CallRequestEvent event, long sequence, boolean endOfBatch) throws Exception {
                stub.call(event);
            }
        });

        downStreamPacketEventDisruptor.handleEventsWith(new EventHandler<PacketEvent>() {
            public void onEvent(PacketEvent event, long sequence, boolean endOfBatch) throws Exception {
                transportHandler.send(event);
            }
        });

        upStreamPacketEventDisruptor.handleEventsWith(new EventHandler<PacketEvent>() {
            public void onEvent(PacketEvent event, long sequence, boolean endOfBatch) throws Exception {
                long seq = callRequestEventRingBuffer.next();
                CallRequestEvent ev = callRequestEventRingBuffer.get(seq);
                protocolHandler.handleCalleeUpstream(event, ev);
                callRequestEventRingBuffer.publish(seq);
            }
        });

        callResponseEventDisruptor.handleEventsWith(new EventHandler<CallResponseEvent>() {
            public void onEvent(CallResponseEvent event, long sequence, boolean endOfBatch) throws Exception {
                long seq = downStreamPacketEventRingBuffer.next();
                PacketEvent ev = downStreamPacketEventRingBuffer.get(seq);
                protocolHandler.handleCalleeDownStream(event, ev);
                downStreamPacketEventRingBuffer.publish(seq);
            }
        });

    }

    public void start() throws IOException {
        callRequestEventRingBuffer = callRequestEventDisruptor.start();
        callResponseEventRingBuffer = callResponseEventDisruptor.start();
        upStreamPacketEventRingBuffer = upStreamPacketEventDisruptor.start();
        downStreamPacketEventRingBuffer = downStreamPacketEventDisruptor.start();
        transportHandler.startServer(ioExecutor, host, port);
    }

    public EventContext<PacketEvent> up() {
        long seq = upStreamPacketEventRingBuffer.next();
        return new EventContext<PacketEvent>(seq, upStreamPacketEventRingBuffer.get(seq));
    }

    public EventContext<CallResponseEvent> down() {
        long seq = callResponseEventRingBuffer.next();
        return new EventContext<CallResponseEvent>(seq, callResponseEventRingBuffer.get(seq));
    }

    public <T> void publish(EventContext<T> eventContext) {
        if (eventContext.getEvent() instanceof PacketEvent) {
            upStreamPacketEventRingBuffer.publish(eventContext.getEventSeq());
            return;
        }
        if (eventContext.getEvent() instanceof CallResponseEvent) {
            callResponseEventRingBuffer.publish(eventContext.getEventSeq());
            return;
        }
        logger.error("invalid event type : {}", eventContext.getEvent().getClass().getName());
    }
}
