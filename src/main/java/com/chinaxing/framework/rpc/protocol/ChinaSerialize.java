package com.chinaxing.framework.rpc.protocol;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 序列化反序列化工具
 * <p/>
 * TODO ：
 * 1. 支持generic
 * 2. 支持注解
 * 3. 支持transient
 * <p/>
 * Created by LambdaCat on 15/8/23.
 */
public class ChinaSerialize {
    private static Map<Class, Integer> classCode = new HashMap<Class, Integer>();
    private static Class[] classIndex = new Class[]{
            int.class, byte.class, char.class, short.class, double.class, float.class, boolean.class, long.class,
            Integer.class, Byte.class, Character.class, Short.class, Double.class, Float.class, Long.class, Boolean.class,
            String.class, Date.class
    };

    private static final DateFormat dateFormat = new SimpleDateFormat();

    static {
        for (int i = 0; i < classIndex.length; i++) {
            classCode.put(classIndex[i], i + 2);
        }
    }

    public final static class DeSerializeResult {
        String name;
        Object value;

        public DeSerializeResult(String name, Object value) {
            this.name = name;
            this.value = value;
        }
    }

    /**
     * 序列化对象
     * TODO 支持有继承关系的类型
     *
     * @param name
     * @param obj
     * @param buffer
     */
    public static void serialize(String name, Object obj, ByteBuffer buffer) {
        Class clz = obj.getClass();
        Integer index = classCode.get(clz);
        byte[] nameByte = name.getBytes();
        buffer.putInt(nameByte.length);
        buffer.put(nameByte);
        if (index != null) {
            buffer.putInt(index);
            if (clz.equals(int.class) || clz.equals(Integer.class)) {
                buffer.putInt(((Integer) obj).intValue());
                return;
            }
            if (clz.equals(byte.class) || clz.equals(Byte.class)) {
                buffer.put(((Byte) obj).byteValue());
                return;
            }
            if (clz.equals(char.class) || clz.equals(Character.class)) {
                buffer.putChar(((Character) obj).charValue());
                return;
            }
            if (clz.equals(short.class) || clz.equals(Short.class)) {
                buffer.putShort(((Short) obj).shortValue());
                return;
            }
            if (clz.equals(long.class) || clz.equals(Long.class)) {
                buffer.putLong(((Long) obj).longValue());
                return;
            }
            if (clz.equals(double.class) || clz.equals(Double.class)) {
                buffer.putDouble(((Double) obj).doubleValue());
                return;
            }
            if (clz.equals(float.class) || clz.equals(Float.class)) {
                buffer.putFloat(((Float) obj).floatValue());
                return;
            }
            if (clz.equals(boolean.class) || clz.equals(Boolean.class)) {
                buffer.put(((Boolean) obj) ? (byte) 1 : (byte) 0);
                return;
            }
            if (clz.equals(String.class)) {
                byte[] sb = ((String) obj).getBytes();
                buffer.putInt(sb.length);
                buffer.put(sb);
                return;
            }
            if (clz.equals(Date.class)) {
                byte[] s = dateFormat.format((Date) obj).getBytes();
                buffer.putInt(s.length);
                buffer.put(s);
                return;
            }
        }


        /**
         * 非原始类型
         */

        if (clz.isArray()) {
            buffer.putInt(0);
            buffer.putInt(((Object[]) obj).length);
            for (int i = 0; i < ((Object[]) obj).length; i++) {
                serialize(String.valueOf(i), ((Object[]) obj)[i], buffer);
            }
            return;
        }


        buffer.putInt(1);
        String clzName = clz.getName();
        byte[] clzNameByte = clzName.getBytes();
        buffer.putInt(clzNameByte.length);
        buffer.put(clzNameByte);
        /**
         * write the Fields
         */
        Class objClz = obj.getClass();
        Field[] fields = objClz.getDeclaredFields();
        List<Field> fieldList = new ArrayList<Field>();
        /**
         * 去掉静态域
         */
        for (Field f : fields) {
            if (Modifier.isStatic(f.getModifiers())) {
                continue;
            }
            fieldList.add(f);
        }
        buffer.putInt(fieldList.size());
        for (Field f : fieldList) {
            try {
                Object of;
                if (!f.isAccessible()) {
                    f.setAccessible(true);
                    of = f.get(obj);
                    f.setAccessible(false);
                } else {
                    of = f.get(obj);
                }
                serialize(f.getName(), of, buffer);
            } catch (IllegalAccessException ie) {
                ie.printStackTrace();
            }
        }
    }

    public static DeSerializeResult deserialize(ByteBuffer buffer) {
        try {
            int len = buffer.getInt();
            byte[] nameByte = new byte[len];
            buffer.get(nameByte);
            String name = new String(nameByte);
            int code = buffer.getInt();
            if (code == 0) { // 数组
                int al = buffer.getInt();
                Object[] a = new Object[al];
                for (int i = 0; i < al; i++) {
                    DeSerializeResult pa = deserialize(buffer);
                    a[i] = pa.value;
                }
                return new DeSerializeResult(name, a);
            }
            if (code == 1) { // 非原始类型
                len = buffer.getInt();
                byte[] clzByte = new byte[len];
                buffer.get(clzByte);
                Class clz = Class.forName(new String(clzByte));
                Object obj = clz.newInstance();
                int fl = buffer.getInt();
                for (int i = 0; i < fl; i++) {
                    DeSerializeResult dp = deserialize(buffer);
                    Field f = clz.getDeclaredField(dp.name);
                    if (f.isAccessible()) {
                        f.set(obj, dp.value);
                    } else {
                        f.setAccessible(true);
                        f.set(obj, dp.value);
                        f.setAccessible(false);
                    }
                }
                return new DeSerializeResult(name, obj);
            }
            Class clz = classIndex[code - 2];
            if (clz.equals(int.class) || clz.equals(Integer.class)) {
                return new DeSerializeResult(name, buffer.getInt());
            }
            if (clz.equals(byte.class) || clz.equals(Byte.class)) {
                return new DeSerializeResult(name, buffer.get());
            }
            if (clz.equals(char.class) || clz.equals(Character.class)) {
                return new DeSerializeResult(name, buffer.getChar());
            }
            if (clz.equals(short.class) || clz.equals(Short.class)) {
                return new DeSerializeResult(name, buffer.getShort());
            }
            if (clz.equals(long.class) || clz.equals(Long.class)) {
                return new DeSerializeResult(name, buffer.getLong());
            }
            if (clz.equals(double.class) || clz.equals(Double.class)) {
                return new DeSerializeResult(name, buffer.getDouble());
            }
            if (clz.equals(float.class) || clz.equals(Float.class)) {
                return new DeSerializeResult(name, buffer.getFloat());
            }
            if (clz.equals(boolean.class) || clz.equals(Boolean.class)) {
                return new DeSerializeResult(name, buffer.get() == 1);
            }
            if (clz.equals(String.class)) {
                int sbl = buffer.getInt();
                byte[] sb = new byte[sbl];
                buffer.get(sb);
                return new DeSerializeResult(name, new String(sb));
            }
            if (clz.equals(Date.class)) {
                int sbl = buffer.getInt();
                byte[] sb = new byte[sbl];
                buffer.get(sb);
                return new DeSerializeResult(name, dateFormat.parse(new String(sb)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static class HellInfo {
        String hello = "world";
        int a = 120;
        byte b = 1;
        WorldInfo worldInfo = new WorldInfo();
    }

    public static class WorldInfo {
        String world = "ye";
        int c = 2;
        Short cc = 22;
    }

    public static void main(String[] args) {
        ByteBuffer buffer = ByteBuffer.allocate(4096);
        serialize("helloworld", new HellInfo(), buffer);
        buffer.flip();
        DeSerializeResult deSerializeResult = deserialize(buffer);
        System.out.println(deSerializeResult.name);
    }
}
