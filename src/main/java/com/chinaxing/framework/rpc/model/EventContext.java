package com.chinaxing.framework.rpc.model;

/**
 * Created by LambdaCat on 15/8/22.
 */
public class EventContext<T> {
    private long eventSeq;
    private T event;

    public EventContext(long eventSeq, T event) {
        this.eventSeq = eventSeq;
        this.event = event;
    }

    public long getEventSeq() {
        return eventSeq;
    }

    public T getEvent() {
        return event;
    }
}
