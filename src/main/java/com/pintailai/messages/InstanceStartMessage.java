package com.pintailai.messages;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.StartEvent;

import java.util.Map;

public class InstanceStartMessage extends AbstractMessage {
    private BpmnModelInstance process;
    private StartEvent startEvent;

    public static InstanceStartMessage createMessage(Map inputData, BpmnModelInstance process, StartEvent startEvent){
        return new InstanceStartMessage(inputData, process, startEvent);
    }

    private InstanceStartMessage(Map inputData, BpmnModelInstance process, StartEvent startEvent){
        super(inputData);
        this.process = process;
        this.startEvent = startEvent;
    }

    public BpmnModelInstance getProcess() {
        return process;
    }

    public StartEvent getStartEvent() {
        return startEvent;
    }
}
