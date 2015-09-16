import com.chinaxing.framework.rpc.ChinaRPC;
import com.chinaxing.framework.rpc.model.WaitType;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by LambdaCat on 15/8/21.
 */
public abstract class TestRPC {
    protected ChinaRPC rpc;

    @Before
    public void testAppointCall() throws Throwable {
        rpc = ChinaRPC.getBuilder().setTimeout(60000)
                .setCallExecutorCount(200)
                .setIoEventLoopCount(1)
                .setWaitType(WaitType.YIELD)
                .build();
    }

    @Test
    public abstract void doTest() throws Throwable;
}
