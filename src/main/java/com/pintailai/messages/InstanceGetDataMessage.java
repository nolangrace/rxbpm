package com.pintailai.messages;

import akka.actor.ActorRef;

import java.util.Map;

public class InstanceGetDataMessage {
    public final long instanceId;
    public final ActorRef originator;

    public static InstanceGetDataMessage createMessage(long instanceId, ActorRef originator){
        return new InstanceGetDataMessage(instanceId, originator);
    }

    public InstanceGetDataMessage(long instanceId, ActorRef originator){
        this.instanceId = instanceId;
        this.originator = originator;
    }
}
