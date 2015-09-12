package impl;

import model.PlainPojo;
import service.ArrayService;

import java.util.Date;

/**
 * Created by LambdaCat on 15/9/12.
 */
public class ArrayServiceImpl implements ArrayService {
    public Integer[] getInts() {
        return new Integer[]{1, 2, 3, 4, 5};
    }

    public short[] getShorts() {
        return new short[]{7, 7, 8, 9, 0};
    }

    public double[] df(double[] x) {
        return new double[]{9.9, 1.2, x[0]};
    }

    public Date[] df(Date[] x) {
        return new Date[]{x[0], new Date()};
    }

    public PlainPojo[] df(PlainPojo[] x) {
        return new PlainPojo[]{new PlainPojo(true, "ok"), new PlainPojo(false, "xxx")};
    }

    public boolean m(PlainPojo[] plainPojos) {
        return false;
    }
}
