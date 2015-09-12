package impl;

import service.MapService;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by LambdaCat on 15/9/12.
 */
public class MapServiceImpl implements MapService {
    public Map<String, Integer> test(Map<Double, Boolean> de) {
        Map<String, Integer> result = new HashMap<String, Integer>();
        String k = de.keySet().toString();
        result.put(k, 11);
        result.put("value", de.values().size());
        return result;
    }
}
