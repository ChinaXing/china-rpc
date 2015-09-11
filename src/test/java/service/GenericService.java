package service;

import java.util.List;
import java.util.Map;

/**
 * 返回和参数是Generic类型
 * <p/>
 * Created by LambdaCat on 15/9/12.
 */
public interface GenericService {
    public List<String> getLs(Map<String, Integer> x);
}
