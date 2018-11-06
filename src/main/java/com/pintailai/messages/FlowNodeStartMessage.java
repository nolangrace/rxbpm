package com.pintailai.messages;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;

import java.util.Map;

public class FlowNodeStartMessage extends AbstractMessage{
    public final String fnId;

    public static FlowNodeStartMessage createMessage(Map inputData, String fnId){
        return new FlowNodeStartMessage(inputData, fnId);
    }

    private FlowNodeStartMessage(Map inputData, String fnId){
        super(inputData);

        this.fnId = fnId;
    }
}
