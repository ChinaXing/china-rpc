package service;

import model.PlainPojo;

/**
 * 返回和参数是普通对象
 * Created by LambdaCat on 15/9/12.
 */
public interface PojoService {
    public PlainPojo getPojo();

    public PlainPojo modify(PlainPojo a);
}
