package model;

import java.util.Date;

/**
 * Created by LambdaCat on 15/8/21.
 */
public class PlainPojo {
    private boolean ok;
    private String msg;
    private int a = 20;
    private Date d;

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getA() {
        return a;
    }

    public void setA(int a) {
        this.a = a;
    }

    public Date getD() {
        return d;
    }

    public void setD(Date d) {
        this.d = d;
    }

    public PlainPojo() {
    }

    public PlainPojo(boolean ok, String msg) {
        this.ok = ok;
        this.msg = msg;
    }

    @Override
    public String toString() {
        return "model.pojo{" +
                "ok=" + ok +
                ", msg='" + msg + '\'' +
                '}';
    }
}
