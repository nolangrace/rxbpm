package com.pintailai.process;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.pintailai.messages.FlowNodeCompleteMessage;
import com.pintailai.messages.FlowNodeStartMessage;
import com.pintailai.messages.InstanceStartMessage;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.camunda.bpm.model.bpmn.instance.StartEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ProcessInstance extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final ActorSystem system = getContext().getSystem();
    private final BpmnModelInstance process;
    private final String instanceId;
    private Map instanceData;

    static Props props(BpmnModelInstance process, String instanceId) {
        return Props.create(ProcessInstance.class, () -> new ProcessInstance(process, instanceId));
    }

    private ProcessInstance(BpmnModelInstance process, String instanceId){
        this.process = process;
        this.instanceId = instanceId;

        instanceData = new HashMap();
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(InstanceStartMessage.class, message -> {
                    StartEvent startEvent = message.getStartEvent();

                    instanceData = message.getData();

                    createAndTriggerFlowNodeActor(startEvent);
                })
                .match(FlowNodeCompleteMessage.class, message -> {
                    // map output data to instance data
                    Map outputData = message.getData();
                    outputData.keySet().forEach(key -> {
                        if(instanceData.containsKey(key))
                            instanceData.replace(key, outputData.get(key));
                        else
                            instanceData.put(key, outputData.get(key));
                    });

                    // create task actor for each nextflownode
                    // if end event check active tasks list if not more active tasks (Not sure what to do)
                    // options: keep instance in memory to get data(passivation) or delete actor
                    message.getNextFlowNodes().forEach(fn -> createAndTriggerFlowNodeActor(fn));
                })
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }

    private void createAndTriggerFlowNodeActor(FlowNode fn){
        UUID uuid = UUID.randomUUID();
        String newTaskId = uuid.toString();

        if(fn instanceof ServiceTask){
            ActorRef ServiceTaskActor = system.actorOf(com.pintailai.flownode.InternalTaskInstance.
                    props(fn, process, newTaskId), "InternalTaskInstance-"+fn.getName()
                    .replace(" ", "_")+"-"+newTaskId);

            ServiceTaskActor.tell(FlowNodeStartMessage.createMessage(instanceData,fn,process),getSelf());
        } else if(fn instanceof ExclusiveGateway){
            ActorRef ExclusiveGatewayActor = system.actorOf(com.pintailai.flownode.gateway.
                    ExclusiveGatewayInstance.props(fn, process, newTaskId), "ExclusiveGatewayInstance-"+fn.getName()
                    .replace(" ", "_")+"-"+newTaskId);

            ExclusiveGatewayActor.tell(FlowNodeStartMessage.createMessage(instanceData,fn,process),getSelf());
        } else {
            log.info("Type of FN not found");
            ActorRef ServiceTaskActor = system.actorOf(com.pintailai.flownode.InternalTaskInstance.
                    props(fn, process, newTaskId), "InternalTaskInstance-"+fn.getName()
                    .replace(" ", "_")+"-"+newTaskId);

            ServiceTaskActor.tell(FlowNodeStartMessage.createMessage(instanceData,fn,process),getSelf());
        }
    }
}
