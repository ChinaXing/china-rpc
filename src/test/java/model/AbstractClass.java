package model;

import java.util.Date;

/**
 * Created by LambdaCat on 15/9/12.
 */
public abstract class AbstractClass {
    private Integer a = 20;
    private Date now = new Date();

    public Integer getA() {
        return a;
    }

    public void setA(Integer a) {
        this.a = a;
    }

    public Date getNow() {
        return now;
    }

    public void setNow(Date now) {
        this.now = now;
    }
}
