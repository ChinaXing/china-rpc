package service;

import model.AbstractClass;

/**
 * 返回和参数类型是抽象类
 * <p/>
 * Created by LambdaCat on 15/9/12.
 */
public interface AbstractService {
    public AbstractClass getAC();

    public AbstractClass set(AbstractClass t);
}
