package impl;

import model.TestPojo;
import service.RpcPerfService;

import java.util.List;

public class RpcPerfServiceImpl implements RpcPerfService {

    public String testStr(String str) {
//        try {
//            Thread.currentThread().sleep(1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        return Thread.currentThread().getName() + ":" + str;
    }

    public TestPojo testPojoIn(TestPojo pojo) {
        return pojo;
    }

    public List<TestPojo> testPojoList(List<TestPojo> pojolist) {
        return pojolist;
    }
}
