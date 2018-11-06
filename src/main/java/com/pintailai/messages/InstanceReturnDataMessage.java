package com.pintailai.messages;

import akka.actor.Actor;
import akka.actor.ActorRef;

import java.util.Map;

public class InstanceReturnDataMessage {
    public final Map instanceData;
    public final ActorRef originator;

    public static InstanceReturnDataMessage createMessage(Map data, ActorRef originator){
        return new InstanceReturnDataMessage(data,originator);
    }

    public InstanceReturnDataMessage(Map instanceData, ActorRef originator){
        this.instanceData = instanceData;
        this.originator = originator;
    }
}
