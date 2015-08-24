package com.chinaxing.framework.rpc.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.rmi.server.ExportException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * 连接管理器
 * Created by LambdaCat on 15/8/24.
 */
public class ConnectionManager {
    private static final ConcurrentHashMap<String, Connection> channelMap =
            new ConcurrentHashMap<String, Connection>();
    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    private static Executor ioExecutor;

    public static void setIoExecutor(Executor ioExecutor) {
        ConnectionManager.ioExecutor = ioExecutor;
    }

    /**
     * 获取连接
     *
     * @param destination
     * @return
     * @throws IOException
     */

    public static Connection getConnection(String destination) throws IOException {
        Connection connection = channelMap.get(destination);
        if (connection == null) {
            synchronized (("ConnectionManager." + destination).intern()) {
                connection = channelMap.get(destination);
                if (connection == null) {
                    connection = new Connection(destination, ioExecutor);
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
    public static void closeConnection(String destination) throws IOException {
        Connection connection = channelMap.remove(destination);
        if (connection != null) {
            connection.close();
        }
    }


    public static void addConnection(String destination, Connection connection) {
        channelMap.put(destination, connection);
    }

    public static Connection addConnection(SocketChannel channel) throws IOException {
        InetSocketAddress socketAddress = (InetSocketAddress) channel.getRemoteAddress();
        String destination = socketAddress.getAddress().getHostAddress() + ":" + socketAddress.getPort();
        Connection connection = new Connection(destination, channel, ioExecutor);
        channelMap.put(destination, connection);
        return connection;
    }
}
