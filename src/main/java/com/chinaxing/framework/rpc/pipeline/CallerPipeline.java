package com.chinaxing.framework.rpc.pipeline;

import com.chinaxing.framework.rpc.DefaultExceptionHandler;
import com.chinaxing.framework.rpc.model.CallRequestEvent;
import com.chinaxing.framework.rpc.model.CallResponseEvent;
import com.chinaxing.framework.rpc.model.EventContext;
import com.chinaxing.framework.rpc.model.PacketEvent;
import com.chinaxing.framework.rpc.protocol.ProtocolHandler;
import com.chinaxing.framework.rpc.stub.CallerStub;
import com.chinaxing.framework.rpc.transport.IoEventLoopGroup;
import com.chinaxing.framework.rpc.transport.LoadBalance;
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
    private TransportHandler transportHandler;
    private CallerStub stub;
    private RingBuffer<CallRequestEvent> callRequestEventRingBuffer;
    private RingBuffer<PacketEvent> downStreamPacketEventRingBuffer;
    private RingBuffer<PacketEvent> upStreamPacketEventRingBuffer;
    private RingBuffer<CallResponseEvent> callResponseEventRingBuffer;

    public CallerPipeline(Executor executor, IoEventLoopGroup ioEventLoopGroup, int capacity, LoadBalance loadBalance) {
        this.executor = executor;
        this.capacity = capacity;
        this.loadBalance = loadBalance;
        transportHandler = new TransportHandler(this, ioEventLoopGroup, loadBalance);
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

        /**
         * 请求的第一环节——数据序列化
         *
         * 如果序列化失败，则以Exception 响应调用者
         */
        callRequestEventDisruptor.handleEventsWith(new EventHandler<CallRequestEvent>() {
            private boolean unPublished = false;
            private long prev;

            public void onEvent(CallRequestEvent event, long sequence, boolean endOfBatch) throws Exception {
                long seq;
                if (unPublished) {
                    seq = prev;
                } else {
                    seq = downStreamPacketEventRingBuffer.next();
                }
                PacketEvent packetEvent = downStreamPacketEventRingBuffer.get(seq);
                try {
                    protocolHandler.handleCallerDownStream(event, packetEvent);
                } catch (Throwable t) {
                    if (!unPublished) {
                        unPublished = true;
                        prev = seq;
                    }
                    CallResponseEvent resp = new CallResponseEvent();
                    resp.setId(event.getId());
                    resp.setException(t);
                    stub.response(resp);
                    return;
                }
                downStreamPacketEventRingBuffer.publish(seq);
                if (unPublished) unPublished = false;
            }
        });

        /**
         * 请求下发的最后一环——数据传输
         *
         * 如果发送失败，则以Exception 响应调用者
         */
        downStreamPacketEventDisruptor.handleEventsWith(new EventHandler<PacketEvent>() {
            public void onEvent(PacketEvent event, long sequence, boolean endOfBatch) throws Exception {
                try {
                    transportHandler.send(event);
                } catch (Throwable t) {
                    CallResponseEvent resp = new CallResponseEvent();
                    resp.setId(event.getId());
                    resp.setException(t);
                    stub.response(resp);
                }
            }
        });

        /**
         * 请求响应
         *
         * 如果解析响应失败，则以Exception 响应
         */
        upStreamPacketEventDisruptor.handleEventsWith(new EventHandler<PacketEvent>() {
            private boolean unPublished = false;
            private long prev;

            public void onEvent(PacketEvent event, long sequence, boolean endOfBatch) throws Exception {
                long seq;
                if (unPublished) {
                    seq = prev;
                } else {
                    seq = callResponseEventRingBuffer.next();
                }
                CallResponseEvent ev = callResponseEventRingBuffer.get(seq);
                try {
                    protocolHandler.handleCallerUpstream(event, ev);
                } catch (Throwable t) {
                    if (!unPublished) {
                        unPublished = true;
                        prev = seq;
                    }
                    logger.error("protocolHandler exception : {}", event);
                    logger.error(" ", t);
                    if (ev.getId() == -1)
                        return;
                    ev.setException(t);
                }
                callResponseEventRingBuffer.publish(seq);
                if (unPublished) unPublished = false;
            }
        });

        /**
         * 调用响应的最后一环——stub
         */
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
