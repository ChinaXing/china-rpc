package com.chinaxing.framework.rpc.protocol;

import com.chinaxing.framework.rpc.model.CallRequestEvent;
import com.chinaxing.framework.rpc.model.CallResponseEvent;
import com.chinaxing.framework.rpc.model.PacketEvent;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
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

    public void handleCallerDownStream(CallRequestEvent requestEvent, PacketEvent event) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
        buffer.putInt(requestEvent.getId());
        byte[] clzNameByte = requestEvent.getClz().getName().getBytes();
        buffer.putInt(clzNameByte.length);
        buffer.put(clzNameByte);
        byte[] methodNameByte = requestEvent.getMethod().getName().getBytes();
        buffer.putInt(methodNameByte.length);
        buffer.put(methodNameByte);
        Class<?>[] parameterTypes = requestEvent.getMethod().getParameterTypes();
        buffer.putInt(parameterTypes.length);
        for (Class c : parameterTypes) {
            byte[] b = c.getName().getBytes();
            buffer.putInt(b.length);
            buffer.put(b);
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
        buffer.flip();
        event.setBuffer(buffer);
    }

    public void handleCallerUpstream(PacketEvent packetEvent, CallResponseEvent responseEvent) {
        /**
         *发送请求失败
         *
         * 发生得原因：
         * 1. 发送由于网络等原因失败，未能发送到对方
         *
         * 处理策略：
         * 1. 上层对上层应用返回Null，同时打印出错误日志和堆栈
         */
        if (packetEvent.getException() != null) {
            responseEvent.setException(packetEvent.getException());
            return;
        }
        ByteBuffer buffer = packetEvent.getBuffer();
        int id = buffer.getInt();
        ChinaSerialize.DeSerializeResult dr = ChinaSerialize.deserialize(buffer);
        responseEvent.setId(id);
        responseEvent.setDestination(packetEvent.getDestination());
        responseEvent.setValue(dr.value);

    }

    public void handleCalleeDownStream(CallResponseEvent responseEvent, PacketEvent event) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
        buffer.putInt(responseEvent.getId());
        ChinaSerialize.serialize("result", responseEvent.getValue(), buffer);
        buffer.flip();
        event.setBuffer(buffer);
        event.setDestination(responseEvent.getDestination());
    }

    public void handleCalleeUpstream(PacketEvent packetEvent, CallRequestEvent requestEvent) {
        /**
         * 调用回应出错（网络失败等发送应答给对方失败）
         *
         * 服务端上层的处理策略：
         * 1. 打印日志，客户端将会超时
         */
        if (packetEvent.getException() != null) {
            requestEvent.setException(packetEvent.getException());
            return;
        }
        try {
            ByteBuffer buffer = packetEvent.getBuffer();
            int id = buffer.getInt();
            int clzL = buffer.getInt();
            byte[] clzNameB = new byte[clzL];
            buffer.get(clzNameB);
            String clzName = new String(clzNameB);
            Class clz = Class.forName(clzName);
            int mL = buffer.getInt();
            byte[] mB = new byte[mL];
            buffer.get(mB);
            String methodName = new String(mB);
            int pl = buffer.getInt();
            Class<?>[] argCls = new Class<?>[pl];
            for (int i = 0; i < pl; i++) {
                int aCL = buffer.getInt();
                byte[] aCB = new byte[aCL];
                buffer.get(aCB);
                String aCN = new String(aCB);
                Class aC = getClass(aCN);
                argCls[i] = aC;
            }
            Method method = clz.getMethod(methodName, argCls);
            requestEvent.setId(id);
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
            requestEvent.setArguments(argCls);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Class getClass(String name) throws ClassNotFoundException {
        Class c = primitiveClassNameMap.get(name);
        if (c == null) {
            return Class.forName(name);
        }
        return c;
    }
}
