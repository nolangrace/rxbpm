package com.pintailai.messages;

import com.pintailai.process.ProcessInstance;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;

import java.util.Map;

public class FlowNodeStartMessage extends AbstractMessage{
    private FlowNode fn;
    private BpmnModelInstance process;

    public static FlowNodeStartMessage createMessage(Map inputData, FlowNode fn, BpmnModelInstance process){
        return new FlowNodeStartMessage(inputData, fn, process);
    }

    private FlowNodeStartMessage(Map inputData, FlowNode fn, BpmnModelInstance process){
        super(inputData);

        this.fn = fn;
        this.process = process;
    }
}
