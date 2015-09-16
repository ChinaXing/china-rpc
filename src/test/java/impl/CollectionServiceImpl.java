package impl;

import model.PlainPojo;
import service.CollectionService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by LambdaCat on 15/9/12.
 */
public class CollectionServiceImpl implements CollectionService {
    public List<Double> test(Set<PlainPojo> a) {
        System.out.println(a.size());
        System.out.println(a.iterator().next().getA());
        List<Double> x = new ArrayList<Double>();
        x.add(10.0);
        x.add(2000.111);
        return x;
    }
}
