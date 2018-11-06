package com.pintailai.messages;

import akka.actor.ActorRef;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class InstanceStartMessage extends AbstractMessage {
    public final String startEventId;
    public final long instanceId;
    public final ActorRef originator;
    public final String processModelString;

    public static InstanceStartMessage createMessage(Map inputData, String startEventId, String processModelString,
                                                     ActorRef originator){
        return new InstanceStartMessage(inputData, startEventId, processModelString, originator);
    }

    public static InstanceStartMessage createMessage(long instanceId, Map inputData, String startEventId, String processModelString,
                                                     ActorRef originator){
        return new InstanceStartMessage(instanceId, inputData, startEventId, processModelString, originator);
    }

    private InstanceStartMessage(Map inputData, String startEventId, String processModelString, ActorRef originator){
        super(inputData);
        this.instanceId = new Random().nextLong();
        this.startEventId = startEventId;

        this.originator = originator;
        this.processModelString = processModelString;
    }

    private InstanceStartMessage(long instanceId, Map inputData, String startEventId, String processModelString, ActorRef originator){
        super(inputData);
        this.instanceId = instanceId;
        this.startEventId = startEventId;

        this.originator = originator;
        this.processModelString = processModelString;
    }
}
