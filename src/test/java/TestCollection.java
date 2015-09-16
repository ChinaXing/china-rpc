import impl.CollectionServiceImpl;
import model.PlainPojo;
import service.CollectionService;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by LambdaCat on 15/9/12.
 */
public class TestCollection extends TestRPC {
    @Override
    public void doTest() throws Throwable {
        rpc.export(new CollectionServiceImpl());
        CollectionService ts = rpc.appointRefer(CollectionService.class, "127.0.0.1:9119");
        Set<PlainPojo> data = new HashSet<PlainPojo>();
        data.add(new PlainPojo(true, "He"));
        System.out.println(ts.test(data).toArray());
    }
}
