package model;

import java.util.List;

/**
 * Created by LambdaCat on 15/9/12.
 */
public class GenericClass<E> {
    E data;
    List<E> ld;
    E[] oks;

    public List<E> getLd() {
        return ld;
    }

    public void setLd(List<E> ld) {
        this.ld = ld;
    }

    public E[] getOks() {
        return oks;
    }

    public void setOks(E[] oks) {
        this.oks = oks;
    }

    public E getData() {
        return data;
    }

    public void setData(E data) {
        this.data = data;
    }
}
