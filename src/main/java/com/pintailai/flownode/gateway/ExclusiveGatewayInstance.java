package com.pintailai.flownode.gateway;

import akka.actor.Props;
import com.pintailai.flownode.AbstractFlowNodeInstance;
import com.pintailai.flownode.InternalTaskInstance;
import com.pintailai.messages.FlowNodeCompleteMessage;
import com.pintailai.messages.FlowNodeStartMessage;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.util.Eval;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class ExclusiveGatewayInstance extends AbstractFlowNodeInstance {

    public static Props props(FlowNode fn, BpmnModelInstance process, String taskId) {
        return Props.create(ExclusiveGatewayInstance.class, () -> new ExclusiveGatewayInstance(fn, process, taskId));
    }

    protected ExclusiveGatewayInstance(FlowNode fn, BpmnModelInstance process, String fnId) throws Exception {
        super(fn, process, fnId);
    }

    @Override
    public final Receive createReceive() {
        return receiveBuilder()
                .match(FlowNodeStartMessage.class, (startMessage) -> {
                    log.info("Starting Gateway with input data: "+startMessage.getData().toString()+" For Gateway:"+fn.getName());

                    getSender().tell(FlowNodeCompleteMessage
                            .createMessage(startMessage.getData(),identifyNextFlowNode()),getSelf());

                })
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }

    @Override
    protected ArrayList<FlowNode> identifyNextFlowNode(){
        ExclusiveGateway exclusiveGateway = (ExclusiveGateway) fn;

        ArrayList<SequenceFlow> sequenceFlows = null;

        sequenceFlows = (ArrayList<SequenceFlow>) exclusiveGateway.getOutgoing().stream().filter(sequenceFlow -> {
            if(sequenceFlow.getId().equals(exclusiveGateway.getDefault().getId())) {
                return false;
            }

            Binding sharedData = new Binding();
            GroovyShell shell = new GroovyShell(sharedData);
            log.info("Gateway: "+fnId+" Evaluating Expression: "+sequenceFlow.getRawTextContent());
//            sharedData.setProperty('date', now)

            Boolean result = (Boolean) shell.evaluate(sequenceFlow.getRawTextContent());
            return result;
        }).collect(Collectors.toList());

        if(sequenceFlows.size() == 0) {
            sequenceFlows.add(exclusiveGateway.getDefault());
        }

        ArrayList<FlowNode> nextFlowNodes = (ArrayList<FlowNode>) sequenceFlows.stream().map(sequenceFlow -> {
            return sequenceFlow.getTarget();
        }).collect(Collectors.toList());

        return nextFlowNodes;
    }
}
