package com.chinaxing.framework.rpc.pipeline;

import com.chinaxing.framework.rpc.DefaultExceptionHandler;
import com.chinaxing.framework.rpc.model.*;
import com.chinaxing.framework.rpc.protocol.ProtocolHandler;
import com.chinaxing.framework.rpc.stub.CalleeStub;
import com.chinaxing.framework.rpc.transport.IoEventLoopGroup;
import com.chinaxing.framework.rpc.transport.TransportHandler;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

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
    private final int capacity;
    private final int port;
    private final String host;
    private final int callThreadNum;
    private final WaitType waitType;
    private final int callExecThreadNum;
    private Disruptor<CallRequestEvent> callRequestEventDisruptor;
    private Disruptor<PacketEvent> downStreamPacketEventDisruptor;
    private Disruptor<PacketEvent> upStreamPacketEventDisruptor;
    private Disruptor<CallResponseEvent> callResponseEventDisruptor;
    private ProtocolHandler protocolHandler = new ProtocolHandler();
    private TransportHandler transportHandler;
    private CalleeStub stub;
    private RingBuffer<CallRequestEvent> callRequestEventRingBuffer;
    private RingBuffer<PacketEvent> downStreamPacketEventRingBuffer;
    private RingBuffer<PacketEvent> upStreamPacketEventRingBuffer;
    private RingBuffer<CallResponseEvent> callResponseEventRingBuffer;


    public CalleePipeline(Executor executor, WaitType waitType,
                          int callThreadNum, IoEventLoopGroup ioEventLoopGroup, int capacity, String host, int port) {
        this.executor = executor;
        this.capacity = capacity;
        this.callThreadNum = callThreadNum;
        this.callExecThreadNum = callThreadNum - 3;
        this.host = host;
        this.port = port;
        this.waitType = waitType;
        transportHandler = new TransportHandler(this, ioEventLoopGroup, null);
        init();
    }

    public void setStub(CalleeStub stub) {
        this.stub = stub;
    }

    private void init() {
        /**
         * 请求
         */
        upStreamPacketEventDisruptor = new Disruptor<PacketEvent>(new EventFactory<PacketEvent>() {
            public PacketEvent newInstance() {
                return new PacketEvent();
            }
        }, capacity, executor, ProducerType.MULTI, getWaitStrategy());

        callRequestEventDisruptor = new Disruptor<CallRequestEvent>(new EventFactory<CallRequestEvent>() {
            public CallRequestEvent newInstance() {
                return new CallRequestEvent();
            }
        }, capacity, executor, ProducerType.SINGLE, getWaitStrategy());


        /**
         * 应答
         */
        downStreamPacketEventDisruptor = new Disruptor<PacketEvent>(new EventFactory<PacketEvent>() {
            public PacketEvent newInstance() {
                return new PacketEvent();
            }
        }, capacity, executor, ProducerType.MULTI, getWaitStrategy());


        callResponseEventDisruptor = new Disruptor<CallResponseEvent>(new EventFactory<CallResponseEvent>() {
            public CallResponseEvent newInstance() {
                return new CallResponseEvent();
            }
        }, capacity, executor, ProducerType.MULTI, getWaitStrategy());


        /**
         * Exception handle
         */
        callRequestEventDisruptor.handleExceptionsWith(new DefaultExceptionHandler<CallRequestEvent>(callRequestEventDisruptor));
        downStreamPacketEventDisruptor.handleExceptionsWith(new DefaultExceptionHandler<PacketEvent>(downStreamPacketEventDisruptor));
        callResponseEventDisruptor.handleExceptionsWith(new DefaultExceptionHandler<CallResponseEvent>(callResponseEventDisruptor));
        upStreamPacketEventDisruptor.handleExceptionsWith(new DefaultExceptionHandler<PacketEvent>(upStreamPacketEventDisruptor));

        // chain together

        /**
         * 接收请求的第一站——数据的反序列化
         *
         * 如果发生异常，则跟如Id进行决定响应
         */
        upStreamPacketEventDisruptor.handleEventsWith(new EventHandler<PacketEvent>() {
            private boolean unPublished = false;
            private long prev;

            public void onEvent(PacketEvent event, long sequence, boolean endOfBatch) throws Exception {
                long seq;
                if (unPublished) {
                    seq = prev;
                } else {
                    seq = callRequestEventRingBuffer.next();
                }
                CallRequestEvent ev = callRequestEventRingBuffer.get(seq);
                try {
                    protocolHandler.handleCalleeUpstream(event, ev);
                } catch (Throwable t) {
                    if (!unPublished) {
                        unPublished = true;
                        prev = seq;
                    }
                    if (ev.getId() == -1) {
                        logger.error("", t);
                        return;
                    }
                    long seq2 = callResponseEventRingBuffer.next();
                    CallResponseEvent resp = callResponseEventRingBuffer.get(seq2);
                    resp.setDestination(event.getDestination());
                    resp.setId(ev.getId());
                    resp.setException(t);
                    callResponseEventRingBuffer.publish(seq2);
                    return;
                }
                callRequestEventRingBuffer.publish(seq);
                if (unPublished) unPublished = false;
            }
        });

        /**
         * 请求上行的最后一站——执行服务调用
         *
         * 此处需要用到线程池进行并发消费——多消费者
         */
        WorkHandler<CallRequestEvent>[] callRequestEventHandlers = new WorkHandler[callExecThreadNum];
        for (int i = 0; i < callExecThreadNum; i++)
            callRequestEventHandlers[i] = new WorkHandler<CallRequestEvent>() {
                public void onEvent(CallRequestEvent event) throws Exception {
                    stub.call(event);
                }
            };
        callRequestEventDisruptor.handleEventsWithWorkerPool(callRequestEventHandlers);

        /**
         * 响应的最后一站——传输
         * 如果发生异常，则打印日志
         */
        downStreamPacketEventDisruptor.handleEventsWith(new EventHandler<PacketEvent>() {
            public void onEvent(PacketEvent event, long sequence, boolean endOfBatch) throws Exception {
                try {
                    transportHandler.send(event);
                } catch (Throwable t) {
                    logger.error("exception on transport ev: {}", event);
                    logger.error("", t);
                }
            }
        });


        /**
         * 响应的第一站，进行序列化
         *
         * 如果失败，则异常返回给调用者
         */
        callResponseEventDisruptor.handleEventsWith(new EventHandler<CallResponseEvent>() {
            private boolean unPublished = false;
            private long prev;

            public void onEvent(CallResponseEvent event, long sequence, boolean endOfBatch) throws Exception {
                long seq;
                if (unPublished) {
                    seq = prev;
                } else {
                    seq = downStreamPacketEventRingBuffer.next();
                }
                PacketEvent ev = downStreamPacketEventRingBuffer.get(seq);
                try {
                    protocolHandler.handleCalleeDownStream(event, ev);
                } catch (Throwable t) {
                    if (!unPublished) {
                        unPublished = true;
                        prev = seq;
                    }
                    logger.error("exception on protocol ev : {}", event);
                    long seq2 = callResponseEventRingBuffer.next();
                    CallResponseEvent resp = callResponseEventRingBuffer.get(seq2);
                    resp.setId(event.getId());
                    resp.setDestination(event.getDestination());
                    resp.setException(t);
                    callResponseEventRingBuffer.publish(seq2);
                    return;
                }
                downStreamPacketEventRingBuffer.publish(seq);
                if (unPublished) unPublished = false;
            }
        });

    }

    public void start() throws IOException {
        callRequestEventRingBuffer = callRequestEventDisruptor.start();
        callResponseEventRingBuffer = callResponseEventDisruptor.start();
        upStreamPacketEventRingBuffer = upStreamPacketEventDisruptor.start();
        downStreamPacketEventRingBuffer = downStreamPacketEventDisruptor.start();
        transportHandler.startServer(host, port);
    }

    public void shutdown() throws IOException {
        callRequestEventDisruptor.shutdown();
        callResponseEventDisruptor.shutdown();
        upStreamPacketEventDisruptor.shutdown();
        downStreamPacketEventDisruptor.shutdown();
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

    private WaitStrategy getWaitStrategy() {
        switch (waitType) {
            case BLOCK:
                return new BlockingWaitStrategy();
            case YIELD:
                return new YieldingWaitStrategy();
            case LITE_BLOCK:
                return new LiteBlockingWaitStrategy();
            case SLEEP:
                return new SleepingWaitStrategy();
            case SPIN:
                return new BusySpinWaitStrategy();
        }
        return new LiteBlockingWaitStrategy();
    }
}
