package com.chinaxing.framework.rpc.protocol;

import com.chinaxing.framework.rpc.model.CallRequestEvent;
import com.chinaxing.framework.rpc.model.CallResponseEvent;
import com.chinaxing.framework.rpc.model.PacketEvent;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;

/**
 * 协议层处理器
 * TODO：
 * 1. byteBuffer 的缓冲（类似对象缓冲，减少重复申请，可以参考Netty的Recycler）
 * 2. 支持超级对象，如果对象很大，那么1024的大小会被超过，将出现Overflow的错误
 * <p/>
 * Created by LambdaCat on 15/8/22.
 */
public class ProtocolHandler {
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
        event.setBuffer(buffer);
    }

    public void handleCallerUpstream(PacketEvent packetEvent, CallResponseEvent responseEvent) {
        ByteBuffer buffer = packetEvent.getBuffer();
        int id = buffer.getInt();
        ChinaSerialize.DeSerializeResult dr = ChinaSerialize.deserialize(buffer);
        responseEvent.setId(id);
        responseEvent.setValue(dr.value);

    }

    public void handleCalleeDownStream(CallResponseEvent responseEvent, PacketEvent event) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
        buffer.putInt(responseEvent.getId());
        ChinaSerialize.serialize("result", responseEvent.getValue(), buffer);
        event.setBuffer(buffer);
    }

    public void handleCalleeUpstream(PacketEvent packetEvent, CallRequestEvent requestEvent) {
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
                String aCN = new String(aCB);
                Class aC = Class.forName(aCN);
                argCls[i] = aC;
            }
            Method method = clz.getMethod(methodName, argCls);
            requestEvent.setClz(clz);
            requestEvent.setMethod(method);
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
}
