package com.chinaxing.framework.rpc.transport;

import com.chinaxing.framework.rpc.model.PacketEvent;

/**
 * 负责发送数据和接受数据
 * 采用NIO，Selector，服务端采用actor模型
 * 1. Acceptor 一个线程
 * 2. 每个connection 分配一个进程（connection不会太多，所以还好）
 * Created by LambdaCat on 15/8/21.
 */
public class TransportHandler {
    public void send(PacketEvent event) {

    }

    public void receive(PacketEvent event) {

    }

}
