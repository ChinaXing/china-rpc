/*
*Created on 202015/9/14 14:32 
*@author jianyong.shao
*/
package model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Created by jianyong.shao on 2015/9/14.
 *
 * @Desc 一句话说明这个类的作用
 */
public class TestPojo implements Serializable {

private static final long serialVersionUID = 1L;

    private String str = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"; // 100字节
    private int  iii = 1111;
    private long lll = 111111l;
    private boolean bool = true;

    private Map<String,String> mapTest;
    private List<String> listTest;

    public String getStr() {
        return str;
    }

    public void setStr(String str) {
        this.str = str;
    }

    public int getIii() {
        return iii;
    }

    public void setIii(int iii) {
        this.iii = iii;
    }

    public long getLll() {
        return lll;
    }

    public void setLll(long lll) {
        this.lll = lll;
    }

    public boolean isBool() {
        return bool;
    }

    public void setBool(boolean bool) {
        this.bool = bool;
    }

    public Map<String, String> getMapTest() {
        return mapTest;
    }

    public void setMapTest(Map<String, String> mapTest) {
        this.mapTest = mapTest;
    }

    public List<String> getListTest() {
        return listTest;
    }

    public void setListTest(List<String> listTest) {
        this.listTest = listTest;
    }
}
