package com.chinaxing.framework.rpc.protocol;

import sun.io.ByteToCharUnicodeBigUnmarked;

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
 * Created by LambdaCat on 15/9/12.
 */
public class SafeBuffer {
    private final int size;
    private int position = 0;
    List<ByteBuffer> buffers = new ArrayList<ByteBuffer>();
    ByteBuffer current;

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
     * assert bs.size < size
     *
     * @param bs
     */
    public void put(byte[] bs) {
        assert bs.length <= size;
        try {
            current.put(bs);
        } catch (BufferOverflowException e) {
            current.flip();
            position += current.limit();
            buffers.add(current);
            current = alloc();
            current.put(bs);
        }
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
