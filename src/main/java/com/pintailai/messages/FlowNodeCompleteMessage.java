package com.pintailai.messages;

import org.camunda.bpm.model.bpmn.instance.FlowNode;

import java.util.ArrayList;
import java.util.Map;

public class FlowNodeCompleteMessage extends AbstractMessage {
    private ArrayList<FlowNode> nextFlowNodes;

    public static FlowNodeCompleteMessage createMessage(Map outputData, ArrayList<FlowNode> nextFlowNodes){
        return new FlowNodeCompleteMessage(outputData, nextFlowNodes);
    }

    private FlowNodeCompleteMessage(Map outputData, ArrayList<FlowNode> nextFlowNodes){
        super(outputData);
        this.nextFlowNodes = nextFlowNodes;
    }

    public ArrayList<FlowNode> getNextFlowNodes() {
        return nextFlowNodes;
    }
}
