package com.chinaxing.framework.rpc.protocol;

import com.chinaxing.framework.rpc.exception.SerializeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;
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
    private static final Logger logger = LoggerFactory.getLogger(ChinaSerialize.class);
    private static final byte EXCEPTION = -1, NULL = -2, ENUM = -3, ARRAY = -4, OBJECT = -5, COLLECTION = -6, MAP = -7;
    private static Map<Class, Byte> classCode = new HashMap<Class, Byte>();
    private static Class[] classIndex = new Class[]{
            int.class, byte.class, char.class, short.class, double.class, float.class, boolean.class, long.class,
            Integer.class, Byte.class, Character.class, Short.class, Double.class, Float.class, Long.class, Boolean.class,
            String.class, Date.class
    };
    private static Map<String, Class> primitiveClass = new HashMap<String, Class>();

//    private static final DateFormat dateFormat = new SimpleDateFormat();

    static {
        for (byte i = 0; i < classIndex.length; i++) {
            classCode.put(classIndex[i], i);
        }
        for (Class c : classIndex) {
            primitiveClass.put(c.getName(), c);
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
    public static void serialize(String name, Object obj, SafeBuffer buffer) throws Throwable {
        writeString(name, buffer);
        /**
         * 异常
         */
        if (obj instanceof Throwable) {
            buffer.put(EXCEPTION);
            writeString(obj.getClass().getName(), buffer);
            writeString(((Throwable) obj).getMessage(), buffer);
            StackTraceElement[] stackTrace = ((Throwable) obj).getStackTrace();
            serialize("stackTrace", stackTrace, buffer);
            return;
        }
        /**
         * 空对象
         */
        if (obj == null) {
            buffer.put(NULL);
            return;
        }
        Class clz = obj.getClass();
        Byte index = classCode.get(clz);
        if (index != null) {
            serializeBaseClass(index, clz, obj, buffer);
            return;
        }


        /**
         * 枚举
         */
        if (clz.isEnum()) {
            buffer.put(ENUM);
            writeString(clz.getName(), buffer);
            String en = ((Enum) obj).name();
            byte[] enB = en.getBytes();
            buffer.putInt(enB.length);
            buffer.put(enB);
            return;
        }


        /**
         * 非原始类型
         */

        if (clz.isArray()) {
            buffer.put(ARRAY);
            Class eClz = clz.getComponentType();
            writeClassName(eClz, buffer);
            buffer.putInt(Array.getLength(obj));
            for (int i = 0; i < Array.getLength(obj); i++) {
                serialize(String.valueOf(i), Array.get(obj, i), buffer);
            }
            return;
        }

        if (Collection.class.isAssignableFrom(clz)) {
            serializeCollection(clz, obj, buffer);
            return;
        }

        if (Map.class.isAssignableFrom(clz)) {
            serializeMap(clz, obj, buffer);
            return;
        }

        /**
         * 对象
         *
         * TODO 支持有参数的构造器
         */

        serializeObject(clz, obj, buffer);
    }

    private static void serializeMap(Class clz, Object obj, SafeBuffer buffer) throws Throwable {
        buffer.put(MAP);
        writeString(clz.getName(), buffer);
        buffer.putInt(((Map) obj).size());
        Iterator<Map.Entry> iterator = ((Map) obj).entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = iterator.next();
            serialize("k", entry.getKey(), buffer);
            serialize("v", entry.getValue(), buffer);
        }
    }

    private static void serializeObject(Class clz, Object obj, SafeBuffer buffer) throws Throwable {
        buffer.put(OBJECT);
        writeString(clz.getName(), buffer);
        /**
         * write the Fields
         *
         * TODO support inherited fields
         */
        Class objClz = obj.getClass();
        Field[] fields = objClz.getDeclaredFields();
        List<Field> fieldList = new ArrayList<Field>();
        /**
         * 去掉静态域
         */
        for (Field f : fields) {
            int mod = f.getModifiers();
            if (Modifier.isStatic(mod) || Modifier.isTransient(mod)
                    || Modifier.isFinal(mod)) {
                continue;
            }
            fieldList.add(f);
        }
        buffer.putInt(fieldList.size());
        for (Field f : fieldList) {
            Object of;
            if (!f.isAccessible()) {
                f.setAccessible(true);
                of = f.get(obj);
                f.setAccessible(false);
            } else {
                of = f.get(obj);
            }
            serialize(f.getName(), of, buffer);
        }
    }

    private static void serializeBaseClass(byte index,
                                           Class clz, Object obj, SafeBuffer buffer) throws Throwable {
        buffer.put(index);
        if (clz.equals(int.class) || clz.equals(Integer.class)) {
            buffer.putInt(((Integer) obj));
            return;
        }
        if (clz.equals(byte.class) || clz.equals(Byte.class)) {
            buffer.put(((Byte) obj));
            return;
        }
        if (clz.equals(char.class) || clz.equals(Character.class)) {
            buffer.putChar(((Character) obj));
            return;
        }
        if (clz.equals(short.class) || clz.equals(Short.class)) {
            buffer.putShort(((Short) obj));
            return;
        }
        if (clz.equals(long.class) || clz.equals(Long.class)) {
            buffer.putLong(((Long) obj));
            return;
        }
        if (clz.equals(double.class) || clz.equals(Double.class)) {
            buffer.putDouble(((Double) obj));
            return;
        }
        if (clz.equals(float.class) || clz.equals(Float.class)) {
            buffer.putFloat(((Float) obj));
            return;
        }
        if (clz.equals(boolean.class) || clz.equals(Boolean.class)) {
            buffer.put(((Boolean) obj) ? (byte) 1 : (byte) 0);
            return;
        }
        if (clz.equals(String.class)) {
            writeString((String) obj, buffer);
            return;
        }
        if (clz.equals(Date.class)) {
            buffer.putLong(((Date) obj).getTime());
            return;
        }
    }

    private static void serializeCollection(Class clz, Object obj, SafeBuffer buffer) throws Throwable {
        buffer.put(COLLECTION);
        /**
         * generic class name
         */
        writeString(clz.getName(), buffer);
        Object[] elements = ((Collection) obj).toArray();
        buffer.putInt(elements.length);
        int i = 0;
        for (Object o : elements) {
            serialize(String.valueOf(++i), o, buffer);
        }
    }

    private static DeSerializeResult deSerializeCollection(String name, SafeBuffer buffer) throws Throwable {
        Class c = Class.forName(parseString(buffer));
        int size = buffer.getInt();
        Collection instance;
        Constructor cstr = c.getDeclaredConstructor();
        if (cstr.isAccessible()) {
            instance = (Collection) cstr.newInstance();
        } else {
            cstr.setAccessible(true);
            instance = (Collection) cstr.newInstance();
            cstr.setAccessible(false);
        }
        for (int i = 0; i < size; i++) {
            DeSerializeResult e = deserialize(buffer);
            instance.add(e.value);
        }
        return new DeSerializeResult(name, instance);
    }

    private static DeSerializeResult deserializeException(String name, SafeBuffer buffer) throws Throwable {
        /**
         * exception class name
         */
        Class e = Class.forName(parseString(buffer));
        Constructor c;
        boolean withEx = false;
        try {
            c = e.getDeclaredConstructor(String.class);
        } catch (Exception x) {
            c = e.getDeclaredConstructor(Throwable.class, String.class);
            withEx = true;
        }
        /**
         * message
         */
        String msg = parseString(buffer);
        /**
         * stackTrace
         */
        DeSerializeResult stackTrace = deserialize(buffer);
        Throwable t;
        if (c.isAccessible()) {
            if (withEx)
                t = (Throwable) c.newInstance(null, msg);
            else
                t = (Throwable) c.newInstance(msg);
        } else {
            c.setAccessible(true);
            if (withEx)
                t = (Throwable) c.newInstance(null, msg);
            else
                t = (Throwable) c.newInstance(msg);
            c.setAccessible(false);
        }
        t.setStackTrace((StackTraceElement[]) stackTrace.value);
        return new DeSerializeResult(name, t);
    }

    public static DeSerializeResult deserialize(SafeBuffer buffer) throws Throwable {
        String name = parseString(buffer);
        int code = buffer.get();
        /**
         * 异常
         */
        if (code == EXCEPTION) {
            return deserializeException(name, buffer);
        }

        if (code == NULL) {
            return new DeSerializeResult(name, null);
        }
        if (code == ENUM) {
            Class clz = Class.forName(parseString(buffer));
            return new DeSerializeResult(name, Enum.valueOf(clz, parseString(buffer)));
        }
        /**
         * 支持1维数组
         */
        if (code == ARRAY) { // 数组
            Class clz = parseArrayElementClass(buffer);
            int al = buffer.getInt();
            Object a = Array.newInstance(clz, al);
            for (int i = 0; i < al; i++) {
                DeSerializeResult pa = deserialize(buffer);
                Array.set(a, i, pa.value);
            }
            return new DeSerializeResult(name, a);
        }

        if (code == COLLECTION) {
            return deSerializeCollection(name, buffer);
        }

        if (code == MAP) {
            return deserializeMap(name, buffer);
        }

        if (code == OBJECT) { // 非原始类型
            Class clz = Class.forName(parseString(buffer));
            Object obj;
            if (clz.equals(StackTraceElement.class)) {
                Constructor constructor = clz.getDeclaredConstructor(String.class, String.class, String.class, int.class);
                obj = constructor.newInstance("", "", "", 0);
            } else {
                Constructor constructor = clz.getDeclaredConstructor();
                if (!constructor.isAccessible()) {
                    constructor.setAccessible(true);
                    obj = constructor.newInstance();
                    constructor.setAccessible(false);
                } else {
                    obj = constructor.newInstance();
                }
            }
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

        Class clz = classIndex[code];
        if (clz != null) {
            return deSerializeBase(name, clz, buffer);
        }
        throw new SerializeException("unknown code : " + code);
    }

    private static DeSerializeResult deserializeMap(String name, SafeBuffer buffer) throws Throwable {
        Class clz = Class.forName(parseString(buffer));
        int size = buffer.getInt();
        Map instance = (Map) clz.newInstance();
        for (int i = 0; i < size; i++) {
            DeSerializeResult k = deserialize(buffer);
            DeSerializeResult v = deserialize(buffer);
            instance.put(k.value, v.value);
        }

        return new DeSerializeResult(name, instance);
    }

    private static DeSerializeResult deSerializeBase(String name, Class clz, SafeBuffer buffer) throws Throwable {
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
            return new DeSerializeResult(name, parseString(buffer));
        }
        if (clz.equals(Date.class)) {
            return new DeSerializeResult(name, new Date(buffer.getLong()));
        }

        throw new SerializeException("unknown class : " + clz.getName());
    }

    private static Class parseArrayElementClass(SafeBuffer buffer) throws Throwable {
        Byte index = buffer.get();
        if (index >= 0) {
            return classIndex[index];
        }
        return Class.forName(parseString(buffer));
    }

    private static void writeClassName(Class clz, SafeBuffer buffer) {
        byte index = getClassIndex(clz);
        buffer.put(index);
        if (index < 0) {
            writeString(clz.getName(), buffer);
        }
    }

    public static void writeString(String str, SafeBuffer buffer) {
        if (str == null) {
            buffer.putInt(0);
            return;
        }
        byte[] clzNameByte = str.getBytes();
        buffer.putInt(clzNameByte.length);
        buffer.put(clzNameByte);
    }

    public static String parseString(SafeBuffer buffer) {
        int sbl = buffer.getInt();
        byte[] sb = new byte[sbl];
        buffer.get(sb);
        return new String(sb);
    }

    private static byte getClassIndex(Class clz) {
        Byte index = classCode.get(clz);
        if (index != null) {
            return index;
        }
        if (clz.isEnum()) return ENUM;
        if (clz.isArray()) return ARRAY;
        return OBJECT;
    }

    public static void main(String[] args) {
        int[] x = new int[]{20, 203, 44};
        byte[] b = new byte[1024];
        Integer[] y = new Integer[]{12, 33};
        SafeBuffer buffer = new SafeBuffer(1024);
        try {
            serialize("test", x, buffer);
            DeSerializeResult r = deserialize(buffer);
            System.out.println(r.name);
            System.out.println(r.value);
            buffer.clear();

            serialize("test", y, buffer);
            r = deserialize(buffer);
            System.out.println(r.name);
            System.out.println(r.value);
            buffer.clear();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
