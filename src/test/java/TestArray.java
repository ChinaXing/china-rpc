import impl.ArrayServiceImpl;
import model.PlainPojo;
import service.ArrayService;

/**
 * Created by LambdaCat on 15/9/12.
 */
public class TestArray extends TestRPC {
    public static void main(String[] args) {
        try {
            new TestArray().doTest();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    public void doTest() throws Throwable {
        rpc.export(new ArrayServiceImpl());
        ArrayService ts = rpc.appointRefer(ArrayService.class, "127.0.0.1:9119");
//        System.out.println(ts.getInts());
//        System.out.println(ts.getShorts());
//        System.out.println(ts.df(new double[]{1.2, 2.3}));
        // System.out.println(ts.df(new Date[]{new Date(), new Date()}));
        System.out.println(ts.m(new PlainPojo[]{new PlainPojo(true, "xxx"), new PlainPojo(false, "xxxDDD")}));
    }
}
