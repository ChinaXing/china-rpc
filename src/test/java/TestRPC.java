import com.chinaxing.framework.rpc.ChinaRPC;
import impl.ArrayServiceImpl;
import org.junit.Before;
import org.junit.Test;
import service.ArrayService;

import java.util.concurrent.Executors;

/**
 * Created by LambdaCat on 15/8/21.
 */
public abstract class TestRPC {
    protected ChinaRPC rpc;

    @Before
    public void testAppointCall() throws Throwable {
        rpc = ChinaRPC.getBuilder().setTimeout(300000)
                .setCallExecutor(Executors.newFixedThreadPool(20))
                .setIoExecutor(Executors.newFixedThreadPool(4))
                .build();
    }

    @Test
    public abstract void doTest() throws Throwable;
}
