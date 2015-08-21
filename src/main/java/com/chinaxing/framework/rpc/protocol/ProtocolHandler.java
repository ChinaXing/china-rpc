package com.chinaxing.framework.rpc.protocol;

import com.chinaxing.framework.rpc.model.CallRequestEvent;
import com.chinaxing.framework.rpc.model.CallResponseEvent;
import com.chinaxing.framework.rpc.model.PacketEvent;

/**
 * Created by LambdaCat on 15/8/22.
 */
public class ProtocolHandler {
    public void handleCallerDownStream(CallRequestEvent requestEvent, PacketEvent event) {

    }

    public void handleCallerUpstream(PacketEvent packetEvent, CallResponseEvent responseEvent) {

    }

    public void handleCalleeDownStream(CallResponseEvent responseEvent, PacketEvent event) {

    }

    public void handleCalleeUpstream(PacketEvent packetEvent, CallRequestEvent requestEvent) {

    }
}
