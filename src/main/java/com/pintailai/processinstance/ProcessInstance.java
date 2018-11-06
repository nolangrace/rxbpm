package com.pintailai.processinstance;

import akka.actor.*;
import akka.cluster.sharding.ShardRegion;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.persistence.AbstractPersistentActor;

import com.pintailai.messages.FlowNodeCompleteMessage;
import com.pintailai.messages.FlowNodeStartMessage;
import com.pintailai.messages.InstanceStartMessage;

import com.pintailai.messages.InstanceStartedMessage;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.camunda.bpm.model.bpmn.instance.StartEvent;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.time.Duration;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class ProcessInstance extends AbstractPersistentActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final ActorSystem system = getContext().getSystem();
    private BpmnModelInstance process;
    private Random random = new Random();
    private long instanceId = random.nextLong();
    private Map instanceData;

    public static class Get {
        final public long instanceId;

        public Get(long instanceId) {
            this.instanceId = instanceId;
        }
    }

    public static class EntityEnvelope implements Serializable {
        final public long id;
        final public Object payload;

        public EntityEnvelope(long id, Object payload) {
            this.id = id;
            this.payload = payload;
        }
    }

    @Override
    public String persistenceId() {
        return "ProcessInstance-" + instanceId;
    }

    @Override
    public void preStart() throws Exception {
        super.preStart();
        getContext().setReceiveTimeout(Duration.ofSeconds(10));
    }

    void updateState(FlowNodeCompleteMessage completeMessage) {
        // map output data to instance data
        Map outputData = completeMessage.data;
        outputData.keySet().forEach(key -> {
            if(instanceData.containsKey(key))
                instanceData.replace(key, outputData.get(key));
            else
                instanceData.put(key, outputData.get(key));
        });
    }

    @Override
    public Receive createReceiveRecover() {
        return receiveBuilder()
                .match(FlowNodeCompleteMessage.class, this::updateState)
                .build();
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(InstanceStartMessage.class, message -> {
                    log.info("Process Instance received request to start new instance");
                    getSender().tell(InstanceStartedMessage.createMessage(instanceId, message.originator),getSender());

                    process = Bpmn.readModelFromStream(
                            new ByteArrayInputStream(message.processModelString.getBytes()));

                    StartEvent startEvent = process.getModelElementById(message.startEventId);

                    instanceData = message.data;

                    createAndTriggerFlowNodeActor(startEvent.getId());
                })
                .match(FlowNodeCompleteMessage.class, this::receiveFlowNodeComplete)
                .matchEquals(ReceiveTimeout.getInstance(), msg -> passivate())
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }

    private void receiveFlowNodeComplete(FlowNodeCompleteMessage completeMessage){
        persist(completeMessage, this::updateState);

        // create task actor for each nextflownode
        // if end event check active tasks list if not more active tasks (Not sure what to do)
        // options: keep instance in memory to get data(passivation) or delete actor
        completeMessage.getNextFlowNodes()
                .forEach(fnId -> createAndTriggerFlowNodeActor(fnId));
    }

    private void createAndTriggerFlowNodeActor(String fnId){
        UUID uuid = UUID.randomUUID();
        String newTaskId = uuid.toString();

        FlowNode fn = process.getModelElementById(fnId);

        if(fn instanceof ServiceTask){
            ActorRef ServiceTaskActor = system.actorOf(com.pintailai.flownode.InternalTaskInstance.
                    props(fn, process, newTaskId), "InternalTaskInstance-"+fn.getName()
                    .replace(" ", "_")+"-"+newTaskId);

            ServiceTaskActor.tell(FlowNodeStartMessage.createMessage(instanceData,fnId),getSelf());
        } else if(fn instanceof ExclusiveGateway){
            ActorRef ExclusiveGatewayActor = system.actorOf(com.pintailai.flownode.gateway.
                    ExclusiveGatewayInstance.props(fn, process, newTaskId), "ExclusiveGatewayInstance-"+fn.getName()
                    .replace(" ", "_")+"-"+newTaskId);

            ExclusiveGatewayActor.tell(FlowNodeStartMessage.createMessage(instanceData,fnId),getSelf());
        } else {
            log.info("Type of FN not found");
            ActorRef ServiceTaskActor = system.actorOf(com.pintailai.flownode.InternalTaskInstance.
                    props(fn, process, newTaskId), "InternalTaskInstance-"+fn.getName()
                    .replace(" ", "_")+"-"+newTaskId);

            ServiceTaskActor.tell(FlowNodeStartMessage.createMessage(instanceData,fnId),getSelf());
        }
    }

    private void passivate() {
        log.info("Passivate instance "+instanceId);
        getContext().getParent().tell(
                new ShardRegion.Passivate(PoisonPill.getInstance()), getSelf());
    }
}
