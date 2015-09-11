package model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by LambdaCat on 15/9/12.
 */
public class ComplexClass {
    private Integer n = 20;
    private double d = 2.3;
    private String name = "OK";
    private List<String> aL = new ArrayList<String>();
    private Date[] ds;
    private short[] sa;
    private Map<Integer, Double> mI;
    private Object obj;
    private AbstractClass ok;
    private InterfaceClass ix;

    public Integer getN() {
        return n;
    }

    public void setN(Integer n) {
        this.n = n;
    }

    public double getD() {
        return d;
    }

    public void setD(double d) {
        this.d = d;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getaL() {
        return aL;
    }

    public void setaL(List<String> aL) {
        this.aL = aL;
    }

    public Date[] getDs() {
        return ds;
    }

    public void setDs(Date[] ds) {
        this.ds = ds;
    }

    public short[] getSa() {
        return sa;
    }

    public void setSa(short[] sa) {
        this.sa = sa;
    }

    public Map<Integer, Double> getmI() {
        return mI;
    }

    public void setmI(Map<Integer, Double> mI) {
        this.mI = mI;
    }

    public Object getObj() {
        return obj;
    }

    public void setObj(Object obj) {
        this.obj = obj;
    }

    public AbstractClass getOk() {
        return ok;
    }

    public void setOk(AbstractClass ok) {
        this.ok = ok;
    }

    public InterfaceClass getIx() {
        return ix;
    }

    public void setIx(InterfaceClass ix) {
        this.ix = ix;
    }
}
