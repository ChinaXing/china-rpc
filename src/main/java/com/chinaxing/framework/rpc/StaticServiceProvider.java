package com.chinaxing.framework.rpc;

import com.chinaxing.framework.rpc.stub.ServiceProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by LambdaCat on 15/8/20.
 */
public class StaticServiceProvider implements ServiceProvider {
    private final Map<String, List<String>> providers = new HashMap<String, List<String>>();

    public Map<String, List<String>> getProvider() {
        return providers;
    }

    public void provide(String clzName, String address) {
        synchronized (providers) {
            List<String> s = providers.get(clzName);
            if (s == null) {
                s = new ArrayList<String>();
                providers.put(clzName, s);
            }
            s.add(address);
        }
    }

    public List<String> getProvider(String service) {
        return providers.get(service);
    }
}
