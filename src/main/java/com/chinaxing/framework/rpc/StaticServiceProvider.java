package com.chinaxing.framework.rpc;

import com.chinaxing.framework.rpc.stub.Provider;
import com.chinaxing.framework.rpc.stub.ServiceProvider;

import java.util.List;

/**
 * Created by LambdaCat on 15/8/20.
 */
public class StaticServiceProvider implements ServiceProvider {
    public StaticServiceProvider(List<String> providers) {
    }

    public List<Provider> allProvider() {
        return null;
    }
}
