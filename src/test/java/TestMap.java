import impl.ExceptionServiceImpl;
import impl.MapServiceImpl;
import service.ExceptionService;
import service.MapService;

import java.util.LinkedHashMap;

/**
 * Created by LambdaCat on 15/9/12.
 */
public class TestMap extends TestRPC {
    @Override
    public void doTest() throws Throwable {
        rpc.export(new MapServiceImpl());
        MapService ts = rpc.appointRefer(MapService.class, "127.0.0.1:9119");
        LinkedHashMap<Double, Boolean> data = new LinkedHashMap<Double, Boolean>();
        data.put(2.2, Boolean.TRUE);
        System.out.println(ts.test(data));
    }
}
