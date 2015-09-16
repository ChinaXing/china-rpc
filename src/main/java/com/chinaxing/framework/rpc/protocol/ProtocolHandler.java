package com.chinaxing.framework.rpc.protocol;

import com.chinaxing.framework.rpc.model.CallRequestEvent;
import com.chinaxing.framework.rpc.model.CallResponseEvent;
import com.chinaxing.framework.rpc.model.PacketEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 协议层处理器
 * TODO：
 * 1. byteBuffer 的缓冲（类似对象缓冲，减少重复申请，可以参考Netty的Recycler）
 * 2. 支持超级对象，如果对象很大，那么1024的大小会被超过，将出现Overflow的错误
 * <p/>
 * Created by LambdaCat on 15/8/22.
 */
public class ProtocolHandler {
    private static final Map<String, Class> primitiveClassNameMap = new HashMap<String, Class>();
    private static final Logger logger = LoggerFactory.getLogger(ProtocolHandler.class);

    static {
        primitiveClassNameMap.put("long", long.class);
        primitiveClassNameMap.put("int", int.class);
        primitiveClassNameMap.put("short", short.class);
        primitiveClassNameMap.put("double", double.class);
        primitiveClassNameMap.put("float", float.class);
        primitiveClassNameMap.put("char", char.class);
        primitiveClassNameMap.put("byte", byte.class);
        primitiveClassNameMap.put("boolean", boolean.class);
    }

    private static Class getClass(String name) throws ClassNotFoundException {
        Class c = primitiveClassNameMap.get(name);
        if (c != null) return c;
        return Class.forName(name);
    }

    public void handleCallerDownStream(CallRequestEvent requestEvent, PacketEvent event) throws Throwable {
        event.setId(requestEvent.getId());
        SafeBuffer buffer = new SafeBuffer(1024);
        buffer.putInt(requestEvent.getId());
        ChinaSerialize.writeString(requestEvent.getClz().getName(), buffer);
        ChinaSerialize.writeString(requestEvent.getMethod().getName(), buffer);
        Class<?>[] parameterTypes = requestEvent.getMethod().getParameterTypes();
        buffer.putInt(parameterTypes.length);
        for (Class c : parameterTypes) {
            ChinaSerialize.writeString(c.getName(), buffer);
        }
        Object[] args = requestEvent.getArguments();
        int al = args.length;
        buffer.putInt(al);
        int i = 0;
        for (Object o : args) {
            ChinaSerialize.serialize("$" + i, o, buffer);
            ++i;
        }
        event.setDestination(requestEvent.getDestination());
        event.setAvailableDestinations(requestEvent.getAvailableDestinations());
        event.setBuffer(buffer);
    }

    /**
     * 调用者收到响应
     *
     * @param packetEvent
     * @param responseEvent
     */
    public void handleCallerUpstream(PacketEvent packetEvent, CallResponseEvent responseEvent) throws Throwable {
        SafeBuffer buffer = packetEvent.getBuffer();
        int id = buffer.getInt();
        responseEvent.setId(id);
        responseEvent.setDestination(packetEvent.getDestination());
        /**
         * value
         */
        ChinaSerialize.DeSerializeResult dr = ChinaSerialize.deserialize(buffer);
        responseEvent.setValue(dr.value);
        /**
         * exception
         */
        dr = ChinaSerialize.deserialize(buffer);
        responseEvent.setException((Throwable) dr.value);
    }

    /**
     * 被调用者（服务方）发送应答
     *
     * @param responseEvent
     * @param event
     */
    public void handleCalleeDownStream(CallResponseEvent responseEvent, PacketEvent event) throws Throwable {
        event.setId(responseEvent.getId());
        SafeBuffer buffer = new SafeBuffer(1024);
        buffer.putInt(responseEvent.getId());
        ChinaSerialize.serialize("result", responseEvent.getValue(), buffer);
        ChinaSerialize.serialize("exception", responseEvent.getException(), buffer);
        event.setBuffer(buffer);
        event.setDestination(responseEvent.getDestination());
    }

    public void handleCalleeUpstream(PacketEvent packetEvent, CallRequestEvent requestEvent) throws Throwable {
        SafeBuffer buffer = packetEvent.getBuffer();
        int id = buffer.getInt();
        requestEvent.setId(id);
        String clzName = ChinaSerialize.parseString(buffer);
        Class clz = Class.forName(clzName);
        String methodName = ChinaSerialize.parseString(buffer);
        int pl = buffer.getInt();
        Class<?>[] argCls = new Class<?>[pl];
        for (int i = 0; i < pl; i++) {
            Class aC = getClass(ChinaSerialize.parseString(buffer));
            argCls[i] = aC;
        }
        Method method = clz.getMethod(methodName, argCls);
        requestEvent.setClz(clz);
        requestEvent.setMethod(method);
        requestEvent.setDestination(packetEvent.getDestination());
        // parse arguments
        int al = buffer.getInt();
        Object[] args = new Object[al];
        for (int i = 0; i < al; i++) {
            ChinaSerialize.DeSerializeResult dr = ChinaSerialize.deserialize(buffer);
            args[i] = dr.value;
        }
        requestEvent.setArguments(args);
    }
}
