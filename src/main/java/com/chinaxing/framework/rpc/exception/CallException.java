package com.chinaxing.framework.rpc.exception;

/**
 * Created by LambdaCat on 15/9/12.
 */
public class CallException extends Throwable {
    public CallException(String reason) {
        super(reason);
    }

    public CallException(String message, Throwable cause) {
        super(message, cause);
    }
}
