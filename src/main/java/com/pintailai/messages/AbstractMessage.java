package com.pintailai.messages;

import org.camunda.bpm.model.bpmn.instance.FlowNode;

import java.util.ArrayList;
import java.util.Map;

public class AbstractMessage {
    private Map data;

    protected AbstractMessage(Map data){
        this.data = data;
    }

    public Map getData() {
        return data;
    }
}
