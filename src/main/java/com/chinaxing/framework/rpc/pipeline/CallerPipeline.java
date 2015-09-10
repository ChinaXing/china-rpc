package com.chinaxing.framework.rpc.pipeline;

import com.chinaxing.framework.rpc.DefaultExceptionHandler;
import com.chinaxing.framework.rpc.model.CallRequestEvent;
import com.chinaxing.framework.rpc.model.CallResponseEvent;
import com.chinaxing.framework.rpc.model.EventContext;
import com.chinaxing.framework.rpc.model.PacketEvent;
import com.chinaxing.framework.rpc.protocol.ProtocolHandler;
import com.chinaxing.framework.rpc.stub.CallerStub;
import com.chinaxing.framework.rpc.transport.LoadBalance;
import com.chinaxing.framework.rpc.transport.RRLoadBalance;
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
 * 调用方的处理链条
 * <p/>
 * <p/>
 * DownStream : Stub ---(CallRequestEvent)---> Protocol ---(PacketEvent)---> Transport
 * <p/>
 * <p/>
 * Upstream : Transport ---(PacketEvent)---> Protocol ---(CallResponseEvent)---> Stub
 * <p/>
 * Created by LambdaCat on 15/8/21.
 */
public class CallerPipeline implements Pipeline<PacketEvent, CallRequestEvent> {
    private static final Logger logger = LoggerFactory.getLogger(CallerPipeline.class);
    private final Executor executor;
    private final int capacity;
    private final LoadBalance loadBalance;
    private Disruptor<CallRequestEvent> callRequestEventDisruptor;
    private Disruptor<PacketEvent> downStreamPacketEventDisruptor;
    private Disruptor<PacketEvent> upStreamPacketEventDisruptor;
    private Disruptor<CallResponseEvent> callResponseEventDisruptor;
    private ProtocolHandler protocolHandler = new ProtocolHandler();
    private TransportHandler transportHandler = new TransportHandler(this);
    private CallerStub stub;
    private RingBuffer<CallRequestEvent> callRequestEventRingBuffer;
    private RingBuffer<PacketEvent> downStreamPacketEventRingBuffer;
    private RingBuffer<PacketEvent> upStreamPacketEventRingBuffer;
    private RingBuffer<CallResponseEvent> callResponseEventRingBuffer;


    public CallerPipeline(Executor executor, int capacity, LoadBalance loadBalance) {
        this.executor = executor;
        this.capacity = capacity;
        this.loadBalance = loadBalance;
        init();
    }

    public void setStub(CallerStub stub) {
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


        callRequestEventDisruptor.handleExceptionsWith(new DefaultExceptionHandler<CallRequestEvent>(callRequestEventDisruptor));
        downStreamPacketEventDisruptor.handleExceptionsWith(new DefaultExceptionHandler<PacketEvent>(downStreamPacketEventDisruptor));
        callResponseEventDisruptor.handleExceptionsWith(new DefaultExceptionHandler<CallResponseEvent>(callResponseEventDisruptor));
        upStreamPacketEventDisruptor.handleExceptionsWith(new DefaultExceptionHandler<PacketEvent>(upStreamPacketEventDisruptor));


        // chain together

        callRequestEventDisruptor.handleEventsWith(new EventHandler<CallRequestEvent>() {
            public void onEvent(CallRequestEvent event, long sequence, boolean endOfBatch) throws Exception {
                long seq = downStreamPacketEventRingBuffer.next();
                PacketEvent packetEvent = downStreamPacketEventRingBuffer.get(seq);
                protocolHandler.handleCallerDownStream(event, packetEvent);
                downStreamPacketEventRingBuffer.publish(seq);
            }
        });

        downStreamPacketEventDisruptor.handleEventsWith(new EventHandler<PacketEvent>() {
            public void onEvent(PacketEvent event, long sequence, boolean endOfBatch) throws Exception {
                transportHandler.send(event);
            }
        });

        upStreamPacketEventDisruptor.handleEventsWith(new EventHandler<PacketEvent>() {
            public void onEvent(PacketEvent event, long sequence, boolean endOfBatch) throws Exception {
                long seq = callResponseEventRingBuffer.next();
                CallResponseEvent ev = callResponseEventRingBuffer.get(seq);
                protocolHandler.handleCallerUpstream(event, ev);
                callResponseEventRingBuffer.publish(seq);
            }
        });

        callResponseEventDisruptor.handleEventsWith(new EventHandler<CallResponseEvent>() {
            public void onEvent(CallResponseEvent event, long sequence, boolean endOfBatch) throws Exception {
                stub.response(event);
            }
        });

    }

    public void start() throws IOException {
        callRequestEventRingBuffer = callRequestEventDisruptor.start();
        callResponseEventRingBuffer = callResponseEventDisruptor.start();
        upStreamPacketEventRingBuffer = upStreamPacketEventDisruptor.start();
        downStreamPacketEventRingBuffer = downStreamPacketEventDisruptor.start();
        transportHandler.startClient(loadBalance);
    }

    public EventContext<PacketEvent> up() {
        long seq = upStreamPacketEventRingBuffer.next();
        return new EventContext<PacketEvent>(seq, upStreamPacketEventRingBuffer.get(seq));
    }

    public EventContext<CallRequestEvent> down() {
        long seq = callRequestEventRingBuffer.next();
        return new EventContext<CallRequestEvent>(seq, callRequestEventRingBuffer.get(seq));
    }

    public <T> void publish(EventContext<T> eventContext) {
        if (eventContext.getEvent() instanceof PacketEvent) {
            upStreamPacketEventRingBuffer.publish(eventContext.getEventSeq());
            return;
        }
        if (eventContext.getEvent() instanceof CallRequestEvent) {
            callRequestEventRingBuffer.publish(eventContext.getEventSeq());
            return;
        }
        logger.error("invalid event type : {}", eventContext.getEvent().getClass().getName());
    }
}
