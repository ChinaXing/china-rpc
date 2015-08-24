package com.chinaxing.framework.rpc.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by LambdaCat on 15/8/24.
 */
public class RRLoadBalance implements LoadBalance {
    private static final Logger logger = LoggerFactory.getLogger(RRLoadBalance.class);
    private final AtomicInteger index = new AtomicInteger(0);
    private final Map<String, Integer> providerInfo = new HashMap<String, Integer>();
    private String current;
    private int count = 0;
    private static final int RR_STICK = 10;
    private static final int MAX_RETRY = 3;

    public synchronized String select(List<String> address) {
        if (current == null) {
            select0(address, address.size());
            return current;
        }
        --count;
        if (count == 0) {
            select0(address, address.size());
        }
        return current;
    }

    public void select0(List<String> address, int len) {
        for (int i = 0; i < len; i++) {
            String dest = address.get(index.getAndIncrement() % len);
            Integer v = providerInfo.get(dest);
            if (v != null && v >= MAX_RETRY) {
                providerInfo.put(dest, v - 1);
                continue;
            }

            try {
                Connection connection = ConnectionManager.getConnection(dest);
                if (!connection.isRunning()) {
                    connection.start();
                }
                current = dest;
                count = RR_STICK;
                providerInfo.put(dest, 0);
                return;
            } catch (Exception e) {
                logger.error("", e);
                if (v == null) {
                    providerInfo.put(dest, 1);
                } else {
                    providerInfo.put(dest, v + 1);
                }
            }
        }
        logger.error("While select from address : {}, cannot do choice", address);
    }


}
