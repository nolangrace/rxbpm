package com.pintailai.flownode;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.xml.type.attribute.Attribute;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class AbstractFlowNodeInstance extends AbstractActor {
    protected final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    protected BpmnModelInstance process;
    protected FlowNode fn;
    protected String className;
    protected String fnId;

    protected AbstractFlowNodeInstance(FlowNode fn, BpmnModelInstance process, String fnId) throws Exception{
        this.fn = fn;
        this.process = process;
        this.fnId = fnId;
        try {
            List<Attribute<?>> attributes = fn.getElementType().getAttributes();
            String implementationType = null;
            for(Attribute<?> attribute:attributes){
                if(attribute.getAttributeName().equals("class")) {
                    className = attribute.getValue(fn).toString();
                }
            }
        } catch (Exception e) {
            log.info("Error");
        }
    }

    @Override
    public abstract Receive createReceive();

    protected ArrayList<FlowNode> identifyNextFlowNode(){
        ArrayList<FlowNode> nextFlowNodes = (ArrayList)fn.getOutgoing().stream().map((flow) -> {
            return flow.getTarget();
        }).collect(Collectors.toList());

        return nextFlowNodes;
    };
}
