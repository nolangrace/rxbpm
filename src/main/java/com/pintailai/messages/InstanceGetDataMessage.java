package com.pintailai.messages;

import java.util.Map;

public class InstanceGetDataMessage {
    public final String instanceId;

    public static InstanceGetDataMessage createMessage(String instanceId){
        return new InstanceGetDataMessage(instanceId);
    }

    public InstanceGetDataMessage(String instanceId){
        this.instanceId = instanceId;
    }
}
