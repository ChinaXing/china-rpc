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
        rpc = ChinaRPC.getBuilder().setTimeout(300000)
                .setCallExecutorCount(10)
                .setIoEventLoopCount(4)
                .setWaitType(WaitType.LITE_BLOCK)
                .build();
    }

    @Test
    public abstract void doTest() throws Throwable;
}
