/*
*Created on 202015/9/14 14:29 
*@author jianyong.shao
*/
package service;

import model.TestPojo;

import java.util.List;

/**
 * Created by jianyong.shao on 2015/9/14.
 *
 * @Desc 一句话说明这个类的作用
 */
public interface RpcPerfService {
    public String testStr(String str);

    public TestPojo testPojoIn(TestPojo pojo);

    public List<TestPojo> testPojoList(List<TestPojo> pojolist);
}
