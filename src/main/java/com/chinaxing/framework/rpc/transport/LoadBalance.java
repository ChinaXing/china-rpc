package com.chinaxing.framework.rpc.transport;

import java.util.List;

/**
 * 选择要被调用的服务端
 * 1. 如果未连接，建立连接
 * 2. 如果连接失败，则选用下一个备选
 * 3. 如果连接断开，则下次重新连接
 * 4. 如果多次轮询都发现一个服务端失败，则减少其选择机会失败数>3 后递减失败数，失败数减为0后再进行选择
 * Created by LambdaCat on 15/8/24.
 */
public interface LoadBalance {
    String select(List<String> address) throws Throwable;

    void setConnectionManager(ConnectionManager connectionManager);
}
