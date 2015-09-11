package model;

/**
 * Created by LambdaCat on 15/9/12.
 */
public class ServiceImplClass implements InterfaceClass {
    private int a = 20;
    private String v = "2";

    public int getA() {
        return a;
    }

    public void setA(int a) {
        this.a = a;
    }

    public String getV() {
        return v;
    }

    public void setV(String v) {
        this.v = v;
    }
}
