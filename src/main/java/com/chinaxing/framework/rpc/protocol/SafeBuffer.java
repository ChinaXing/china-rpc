package com.chinaxing.framework.rpc.protocol;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * 经过封装的安全buffer，自动增长
 * <p/>
 * Buffer中第一个Int代表长度
 * <p/>
 * <p/>
 * 读：读取的时候认为SafeBuffer仅仅有一个ByteBuffer——current，主要是因为网络IO的时候会一次性读进来
 * 写：写入的时候，SafeBuffer有若干个size大小的ByteBuffer组成，写保证安全性——自动按需增长
 * <p/>
 * 获取写入后的buffer list：{@code getBuffers}
 * <p/>
 * <p/>
 * Created by LambdaCat on 15/9/12.
 */
public class SafeBuffer {
    private final int size;
    List<ByteBuffer> buffers = new ArrayList<ByteBuffer>();
    ByteBuffer current;
    private int position = 0;

    public SafeBuffer(int size) {
        this.size = size;
        current = alloc();
        current.putInt(0);
    }

    private ByteBuffer alloc() {
        return ByteBuffer.allocateDirect(size);
    }

    public byte get() {
        return current.get();
    }

    public void get(byte[] b) {
        current.get(b);
    }

    public short getShort() {
        return current.getShort();
    }

    public int getInt() {
        return current.getInt();
    }

    public long getLong() {
        return current.getLong();
    }

    public double getDouble() {
        return current.getDouble();
    }

    public float getFloat() {
        return current.getFloat();
    }

    public char getChar() {
        return current.getChar();
    }

    public byte get(int index) {
        return current.get(index);
    }

    public void put(byte b) {
        try {
            current.put(b);
        } catch (BufferOverflowException e) {
            current.flip();
            position += current.limit();
            buffers.add(current);
            current = alloc();
            current.put(b);
        }
    }

    public void putChar(char c) {
        try {
            current.putChar(c);
        } catch (BufferOverflowException e) {
            current.flip();
            position += current.limit();
            buffers.add(current);
            current = alloc();
            current.putChar(c);
        }
    }

    public void putShort(short s) {
        try {
            current.putShort(s);
        } catch (BufferOverflowException e) {
            current.flip();
            position += current.limit();
            buffers.add(current);
            current = alloc();
            current.putShort(s);
        }
    }

    public void putInt(int i) {
        try {
            current.putInt(i);
        } catch (BufferOverflowException e) {
            current.flip();
            position += current.limit();
            buffers.add(current);
            current = alloc();
            current.putInt(i);
        }
    }

    public void putLong(long i) {
        try {
            current.putLong(i);
        } catch (BufferOverflowException e) {
            current.flip();
            position += current.limit();
            buffers.add(current);
            current = alloc();
            current.putLong(i);
        }
    }

    public void putFloat(float i) {
        try {
            current.putFloat(i);
        } catch (BufferOverflowException e) {
            current.flip();
            position += current.limit();
            buffers.add(current);
            current = alloc();
            current.putFloat(i);
        }
    }

    public void putDouble(double i) {
        try {
            current.putDouble(i);
        } catch (BufferOverflowException e) {
            current.flip();
            position += current.limit();
            buffers.add(current);
            current = alloc();
            current.putDouble(i);
        }
    }

    /**
     * @param bs
     */
    public void put(byte[] bs) {
        put(bs, 0, bs.length);
    }

    public void put(byte[] bs, int from, int len) {
        int capacity = current.remaining();
        if (capacity >= len) {
            current.put(bs, from, len);
            return;
        }
        current.put(bs, from, capacity);
        current.flip();
        position += current.limit();
        buffers.add(current);
        current = alloc();
        put(bs, from + capacity, len - capacity);
    }

    public ByteBuffer[] getBuffers() {
        current.flip();
        position += current.limit();
        int bL = buffers.size();
        ByteBuffer[] result = new ByteBuffer[bL + 1];
        System.arraycopy(buffers.toArray(), 0, result, 0, bL);
        result[bL] = current;
        result[0].putInt(0, position);
        return result;
    }

    public ByteBuffer getMergeRead() {
        current.flip();
        position += current.limit();
        if (position == current.limit()) return current;
        ByteBuffer result = ByteBuffer.allocateDirect(position);
        for (ByteBuffer b : buffers) {
            result.put(b);
        }
        return result;
    }

    public ByteBuffer getWrite() {
        return current;
    }

    public void clear() {
        current.clear();
        buffers = new ArrayList<ByteBuffer>();
    }

    public void put(ByteBuffer b) {
        int rem0 = current.remaining();
        int rem1 = b.remaining();
        if (rem0 >= rem1) {
            current.put(b);
            return;
        }
        int limit = b.limit();
        b.limit(rem0 + b.position());
        current.put(b);
        b.limit(limit);
        current.flip();
        position += current.limit();
        buffers.add(current);
        current = alloc();
        put(b);
    }

    public boolean hasRemaining() {
        return current.hasRemaining();
    }

    public int remaining() {
        return current.remaining();
    }

    public void flip() {
        current.flip();
    }
}
