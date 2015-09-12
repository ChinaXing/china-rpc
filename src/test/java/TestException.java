import impl.ArrayServiceImpl;
import impl.ExceptionServiceImpl;
import service.ArrayService;
import service.ExceptionService;

/**
 * Created by LambdaCat on 15/9/12.
 */
public class TestException extends TestRPC {

    @Override
    public void doTest() throws Throwable {
        rpc.export(new ExceptionServiceImpl());
        ExceptionService ts = rpc.appointRefer(ExceptionService.class, "127.0.0.1:9119");
        ts.TestExcep();
    }
}
