import com.chinaxing.framework.rpc.ChinaRPC;
import com.chinaxing.framework.rpc.model.ServiceInfo;
import org.junit.Test;

/**
 * Created by LambdaCat on 15/8/21.
 */
public class TestRPC {
    @Test
    public void testClusterCall() {
        ChinaRPC rpc = ChinaRPC.getBuilder().addProvider(TimeService.class.getName(), "127.0.0.1").setTimeout(5000).build();
        rpc.export(new TimeServiceImpl());
        try {
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
                TimeService.class.getName(), "127.0.0.1").setTimeout(5000).build();
        rpc.export(new TimeServiceImpl());
        try {
            TimeService ts = rpc.appointRefer(TimeService.class, "127.0.0.1");
            System.out.println(ts.getDate());
            System.out.println(ts.getTimestamp());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
