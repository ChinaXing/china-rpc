package com.chinaxing.framework.rpc.stub;

import java.util.List;
import java.util.Map;

/**
 * 服务提供方获取类
 * Created by LambdaCat on 15/8/20.
 */
public interface ServiceProvider {
    /**
     * 返回服务提供者信息
     * <p/>
     * 服务 -> 地址列表
     *
     * @return
     */
    Map<String, List<String>> getProvider();


    /**
     * 将服务提供出去
     *
     * @param clzName
     */
    void provide(String clzName, String address);

    /**
     * 获取服务的提供者地址
     *
     * @param clzName
     * @return
     */
    List<String> getProvider(String clzName);

}
