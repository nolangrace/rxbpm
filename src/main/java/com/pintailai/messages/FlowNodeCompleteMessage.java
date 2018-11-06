package com.pintailai.messages;

import java.util.ArrayList;
import java.util.Map;

public class FlowNodeCompleteMessage extends AbstractMessage {
    private ArrayList<String> nextFlowNodeIds;

    public static FlowNodeCompleteMessage createMessage(Map outputData, ArrayList<String> nextFlowNodeIds){
        return new FlowNodeCompleteMessage(outputData, nextFlowNodeIds);
    }

    private FlowNodeCompleteMessage(Map outputData, ArrayList<String> nextFlowNodeIds){
        super(outputData);
        this.nextFlowNodeIds = nextFlowNodeIds;
    }

    public ArrayList<String> getNextFlowNodes() {
        return nextFlowNodeIds;
    }
}
