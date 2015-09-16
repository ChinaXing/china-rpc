import impl.RpcPerfServiceImpl;
import service.RpcPerfService;

import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by LambdaCat on 15/9/16.
 */
public class TestConcurrency extends TestRPC {
    private String s;
    private AtomicLong used = new AtomicLong(0);
    private static final int CONCURRENCY = 100;
    private static final int DATA_SIZE = 1024 * 1024;
    private static final AtomicInteger index = new AtomicInteger(0);

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
        buildStr(DATA_SIZE);
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

    private void singleTest(CyclicBarrier startBarrier, CountDownLatch latch) throws Throwable {
        RpcPerfService ts = rpc.appointRefer(RpcPerfService.class, "127.0.0.1:9119");
        startBarrier.await();
        long start = System.nanoTime();
        try {
            ts.testStr(s);
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
