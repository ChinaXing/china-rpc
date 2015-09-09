import com.chinaxing.framework.rpc.ChinaRPC;
import org.junit.Test;

import java.util.concurrent.Executors;

/**
 * Created by LambdaCat on 15/8/21.
 */
public class TestRPC {
    @Test
    public void testClusterCall() {
        ChinaRPC rpc = ChinaRPC.getBuilder().addProvider(TimeService.class.getName(), "127.0.0.1:9119").setTimeout(100000).build();
        try {
            rpc.export(new TimeServiceImpl());
            TimeService ts = rpc.refer(TimeService.class);
            System.out.println(ts.getDate());
            System.out.println(ts.getTimestamp());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Test
    public void testAppointCall() {
        ChinaRPC rpc = ChinaRPC.getBuilder().addProvider(
                TimeService.class.getName(), "127.0.0.1:9119").setTimeout(5000)
                .setCallExecutor(Executors.newFixedThreadPool(1))
                .setIoExecutor(Executors.newFixedThreadPool(1))
                .build();
        try {
            rpc.export(new TimeServiceImpl());
            TimeService ts = rpc.appointRefer(TimeService.class, "127.0.0.1:9119");
            System.out.println(ts.getDate());
            System.out.println(ts.getTimestamp());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
