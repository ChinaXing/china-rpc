import impl.RpcPerfServiceImpl;
import junit.framework.Assert;
import model.TestPojo;
import org.junit.Test;
import service.RpcPerfService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class TestPerf extends TestRPC {

    @Test
    public void strPerfTest() throws Throwable {
        rpc.export(new RpcPerfServiceImpl());
        RpcPerfService ts = rpc.appointRefer(RpcPerfService.class, "127.0.0.1:9119");
        String testStr = makeStrByK(1024);
        int times = 10;
        long starTime = System.nanoTime();
        for (int i = 0; i < times; i++) {
            System.out.println("第" + i + "次 start");
            ts.testStr(testStr);
            System.out.println("第" + i + "次 end");
        }
        long endTime = System.nanoTime();
        System.out.println("use time: " + (endTime - starTime) / 1000 + " us");
        System.out.println("avg time: " + (endTime - starTime) / 1000 / times + " us");
    }

    @Test
    public void testPojoPerf() throws Throwable {
        rpc.export(new RpcPerfServiceImpl());
        RpcPerfService ts = rpc.appointRefer(RpcPerfService.class, "127.0.0.1:9119");
        TestPojo testpojo = new TestPojo();
        HashMap<String, String> testMap = new HashMap<String, String>();
        testMap.put("key", "value");
        testpojo.setMapTest(testMap);
        List<String> listStr = new ArrayList<String>();
        listStr.add("aaaaa");
        testpojo.setListTest(listStr);

        int times = 10000;
        long starTime = System.nanoTime();
        for (int i = 0; i < times; i++) {
            ts.testPojoIn(testpojo);
        }
        long endTime = System.nanoTime();
        System.out.println("use time: " + (endTime - starTime) / 1000 + " us");
        System.out.println("avg time: " + (endTime - starTime) / 1000 / times + " us");

    }


    @Test
    public void testPojoPerfList() throws Throwable {
        rpc.export(new RpcPerfServiceImpl());
        RpcPerfService ts = rpc.appointRefer(RpcPerfService.class, "127.0.0.1:9119");
        TestPojo testpojo = new TestPojo();
        HashMap<String, String> testMap = new HashMap<String, String>();
        testMap.put("key", "value");
        testpojo.setMapTest(testMap);
        List<String> listStr = new ArrayList<String>();
        listStr.add("aaaaa");
        testpojo.setListTest(listStr);

        List<TestPojo> pojolist = new ArrayList<TestPojo>();
        for (int j = 1; j < 100; j++) {
            pojolist.add(testpojo);
        }
        int times = 10000;
        long starTime = System.nanoTime();
        for (int i = 0; i < times; i++) {
//            String resultStr =
            ts.testPojoList(pojolist);
//            Assert.assertEquals("输入与输出值相等",testStr,resultStr);
        }
        long endTime = System.nanoTime();
        System.out.println("use time: " + (endTime - starTime) / 1000 + " us");
        System.out.println("avg time: " + (endTime - starTime) / 1000 / times + " us");

    }

    @Test
    public void testPojo() throws Throwable {
        rpc.export(new RpcPerfServiceImpl());
        RpcPerfService ts = rpc.appointRefer(RpcPerfService.class, "127.0.0.1:9119");
        TestPojo testpojo = new TestPojo();
        HashMap<String, String> testMap = new HashMap<String, String>();
        testMap.put("key", "value");
        testpojo.setMapTest(testMap);
        List<String> listStr = new ArrayList<String>();
        listStr.add("aaaaa");
        testpojo.setListTest(listStr);


        TestPojo responePojo = ts.testPojoIn(testpojo);
        Assert.assertEquals(responePojo.getStr(), testpojo.getStr());
        Assert.assertEquals(responePojo.getIii(), testpojo.getIii());
        Assert.assertEquals(responePojo.getLll(), testpojo.getLll());
        Assert.assertEquals(responePojo.getMapTest(), testpojo.getMapTest());
        Assert.assertEquals(responePojo.getListTest(), testpojo.getListTest());

    }

    /**
     * 生成指定长度字符串
     *
     * @param kb
     * @return
     */
    private String makeStrByK(int kb) {
        byte[] b = new byte[1024];
        Arrays.fill(b, (byte) 0x65);
        String tempStr = new String(b);
        StringBuffer strBuf = new StringBuffer();
        for (int i = 0; i < kb; i++) {
            strBuf.append(tempStr);
        }
        return strBuf.toString();
    }


    @Override
    public void doTest() throws Throwable {

    }
}
