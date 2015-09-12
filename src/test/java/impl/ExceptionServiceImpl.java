package impl;

import service.ExceptionService;

/**
 * Created by LambdaCat on 15/9/12.
 */
public class ExceptionServiceImpl implements ExceptionService {
    int a = 0;

    public void TestExcep() {
        a = 2 / a;
    }
}
