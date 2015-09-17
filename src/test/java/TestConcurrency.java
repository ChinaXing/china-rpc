import impl.RpcPerfServiceImpl;
import model.TestPojo;
import service.RpcPerfService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by LambdaCat on 15/9/16.
 */
public class TestConcurrency extends TestRPC {
    private String s;
    private TestPojo pojo;
    private AtomicLong used = new AtomicLong(0);
    private static int CONCURRENCY = 1;
    private static int DATA_SIZE = 1024 * 1024 * 200;
    private static final AtomicInteger index = new AtomicInteger(0);
    private static final int TEST_STR = 1;
    private static final int TEST_POJO = 1;
    private static int testType = TEST_STR;


    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Miss args");
            return;
        }

        try {
            TestConcurrency tester = new TestConcurrency();
            tester.testAppointCall();
            String t = args[0];
            if ("str".equals(t)) {
                testType = TEST_STR;
                return;
            }
            if ("pojo".equals(t)) {
                testType = TEST_POJO;
            }
            tester.doTest();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        System.out.println("Done");
    }

    @Override
    public void doTest() throws Throwable {
        rpc.export(new RpcPerfServiceImpl());
//        final Semaphore semaphore = new Semaphore(0);
        final CountDownLatch latch = new CountDownLatch(CONCURRENCY);
        final CyclicBarrier barrier = new CyclicBarrier(CONCURRENCY, new Runnable() {
            @Override
            public void run() {
                System.out.println("staring : " + System.currentTimeMillis());
            }
        });
//        final CyclicBarrier barrierEnd = new CyclicBarrier(CONCURRENCY, new Runnable() {
//            @Override
//            public void run() {
//                System.out.println("End : " + System.currentTimeMillis());
//                semaphore.release();
//            }
//        });
        Executor executor = Executors.newFixedThreadPool(CONCURRENCY, new ThreadFactory() {
            private final AtomicInteger index = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "t-" + index.getAndIncrement());
            }
        });
        if (testType == TEST_STR) {
            buildStr(DATA_SIZE);
        } else if (testType == TEST_POJO) {
            buildPojo();
        }
        int i = CONCURRENCY;
        while (i-- > 0) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
//                        singleTest(barrier, barrierEnd);
                        singleTest(barrier, latch);
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                }
            });
        }

        latch.await();
//        semaphore.acquire();
        System.out.println("average : " + used.get() / CONCURRENCY / 1000 + " us");

    }

    private void buildPojo() {
        pojo = new TestPojo();
        pojo.setBool(false);
        pojo.setIii(2233);
        pojo.setLll(99999);
        pojo.setListTest(Arrays.asList("hello", "world", "lambda", "cat", "china", "hangzhou"));
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < pojo.getListTest().size(); i += 2) {
            map.put(pojo.getListTest().get(i), pojo.getListTest().get(i + 1));
        }
        pojo.setMapTest(map);
    }

    private void singleTest(CyclicBarrier startBarrier, CountDownLatch latch) throws Throwable {
        RpcPerfService ts = rpc.appointRefer(RpcPerfService.class, "127.0.0.1:9119");
        startBarrier.await();
        long start = System.nanoTime();
        try {
            if (testType == TEST_STR) {
                ts.testStr(s);
            } else if (testType == TEST_POJO) {
                ts.testPojoIn(pojo);
            } else {
                System.out.println("Test Nothing");
            }
            long end = System.nanoTime();
            long t = end - start;
            System.out.println(index.getAndIncrement() + " " + Thread.currentThread().getName() + " " + t);
            used.addAndGet(t);
        } finally {
            latch.countDown();
        }
    }


    private String buildStr(int size) {
        if (s == null) {
            char[] x = new char[size];
            Arrays.fill(x, 'a');
            s = String.copyValueOf(x);
        }
        return s;
    }
}
