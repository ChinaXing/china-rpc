package com.chinaxing.framework.rpc;

import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.dsl.Disruptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by LambdaCat on 15/9/10.
 */
public class DefaultExceptionHandler<E> implements ExceptionHandler<E> {
    private static final Logger logger = LoggerFactory.getLogger(DefaultExceptionHandler.class);
    private Disruptor<E> disruptor;

    public DefaultExceptionHandler(Disruptor<E> disruptor) {
        this.disruptor = disruptor;
    }

    public void handleEventException(Throwable ex, long sequence, E event) {
        logger.error("[{}] Event Exception : event = {}, ex : {}", disruptor, event, ex);
    }

    public void handleOnStartException(Throwable ex) {
        logger.error("[{}] start Exception : {}", disruptor, ex);
    }

    public void handleOnShutdownException(Throwable ex) {
        logger.error("[{}] start Exception : ", disruptor, ex);
    }
}
