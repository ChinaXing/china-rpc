package service;

import model.PlainPojo;

import java.util.Date;

/**
 * 返回和参数类型是数组
 * <p/>
 * Created by LambdaCat on 15/9/12.
 */
public interface ArrayService {
    public Integer[] getInts();

    public short[] getShorts();

    public double[] df(double[] x);

    public Date[] df(Date[] x);

    public PlainPojo[] df(PlainPojo[] x);

    boolean m(PlainPojo[] plainPojos);
}
