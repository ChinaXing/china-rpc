package impl;

import model.AbstractClass;
import model.SubClass;
import service.AbstractService;

/**
 * Created by LambdaCat on 15/9/12.
 */
public class AbstractServiceImpl implements AbstractService {
    public AbstractClass getAC() {
        return new SubClass();
    }

    public AbstractClass set(AbstractClass t) {
        return t;
    }
}
