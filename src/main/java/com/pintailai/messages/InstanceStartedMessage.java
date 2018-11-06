package com.pintailai.messages;

import akka.actor.ActorRef;

import java.io.Serializable;

public class InstanceStartedMessage implements Serializable {
    public final long instanceId;
    public final ActorRef originator;

    public static InstanceStartedMessage createMessage(long instanceId, ActorRef originator){
        return new InstanceStartedMessage(instanceId, originator);
    }

    private InstanceStartedMessage(long instanceId, ActorRef originator){
        this.instanceId = instanceId;
        this.originator = originator;
    }
}
