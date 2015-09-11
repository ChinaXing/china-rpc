package service;

import model.AbstractClass;
import model.SubClass;

/**
 * 调用父类的公共方法
 * <p/>
 * Created by LambdaCat on 15/9/12.
 */
public interface ExtendsMethodService {
    public SubClass sb(SubClass sb);

    public AbstractClass ab(AbstractClass as);
}
