package com.chinaxing.framework.rpc.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 连接管理器
 * <p/>
 * 单例
 * Created by LambdaCat on 15/8/24.
 */
public class ConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);
    private final ConcurrentHashMap<String, Connection> channelMap =
            new ConcurrentHashMap<String, Connection>();
    private IoEventLoopGroup ioEventLoopGroup;
    private ConnectionHandler connectionHandler;

    public ConnectionManager(IoEventLoopGroup ioEventLoopGroup, ConnectionHandler handler) {
        this.ioEventLoopGroup = ioEventLoopGroup;
        this.connectionHandler = handler;
    }

    /**
     * 获取连接
     *
     * @param destination
     * @return
     * @throws IOException
     */

    public Connection getConnection(String destination) throws Throwable {
        Connection connection = channelMap.get(destination);
        if (connection == null) {
            synchronized (("ConnectionManager." + destination).intern()) {
                connection = channelMap.get(destination);
                if (connection == null) {
                    connection = new Connection(destination, connectionHandler, ioEventLoopGroup.getIoEventLoop());
                    channelMap.put(destination, connection);
                }
            }
        }
        return connection;
    }

    /**
     * 关闭连接
     *
     * @param destination
     * @throws IOException
     */
    public void closeConnection(String destination) {
        Connection connection = channelMap.remove(destination);
        if (connection != null) {
            connection.close();
        }
    }

    public Connection addConnection(SocketChannel channel) throws Throwable {
        InetSocketAddress socketAddress = (InetSocketAddress) channel.getRemoteAddress();
        String destination = socketAddress.getAddress().getHostAddress() + ":" + socketAddress.getPort();
        Connection connection = getConnection(destination);
        connection.setChannel(channel);
        return connection;
    }
}
