package com.pintailai.processinstance;

import akka.actor.*;
import akka.cluster.sharding.ShardRegion;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.persistence.AbstractPersistentActor;

import com.pintailai.messages.*;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.camunda.bpm.model.bpmn.instance.StartEvent;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.time.Duration;
import java.util.*;

public class ProcessInstance extends AbstractPersistentActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final ActorSystem system = getContext().getSystem();
    private BpmnModelInstance process;
    private long instanceId;
    private Map instanceData;

    public static class Get {
        final public long instanceId;

        public Get(long instanceId) {
            this.instanceId = instanceId;
        }
    }

    public static class EntityEnvelope implements Serializable {
        final public long instanceId;
        final public Object payload;

        public EntityEnvelope(long instanceId, Object payload) {
            this.instanceId = instanceId;
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
        getContext().setReceiveTimeout(Duration.ofSeconds(5));
    }

    void setupInstance(InstanceStartEvnt e) {
        instanceId = e.instanceId;

        process = Bpmn.readModelFromStream(
                new ByteArrayInputStream(e.processModelString.getBytes()));

        instanceData = e.data;
    }

    void updateState(TaskCompleteEvnt e) {
        Map data = e.data;

        if(instanceData == null)
            instanceData = new HashMap();

        data.keySet().forEach(key -> {
            if(instanceData.containsKey(key))
                instanceData.replace(key, data.get(key));
            else
                instanceData.put(key, data.get(key));
        });
    }

    @Override
    public Receive createReceiveRecover() {
        log.info("Recovering instance");
        return receiveBuilder()
                .match(InstanceStartEvnt.class, this::setupInstance)
                .match(TaskCompleteEvnt.class,this::updateState)
                .build();
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(InstanceStartMessage.class, message -> {
                    if(instanceId != 0) {
                        getSender().tell(InstanceStartMessage.createMessage(message.data, message.startEventId,
                                message.processModelString, message.originator), message.originator);
                    }
                    else {

                        InstanceStartEvnt evnt = new InstanceStartEvnt(message.instanceId,message.data,
                                message.processModelString);
                        persist(evnt, (e) -> {
                            setupInstance(e);

                            log.info("Process Instance received request to start new instance instanceId:" + instanceId);
                            message.originator.tell(InstanceStartedMessage.createMessage(instanceId, message.originator), getSender());

                            StartEvent startEvent = process.getModelElementById(message.startEventId);

                            createAndTriggerFlowNodeActor(startEvent.getId());
                        });
                    }
                })
                .match(FlowNodeCompleteMessage.class, this::receiveFlowNodeComplete)
                .match(InstanceGetDataMessage.class, this::getInstanceData)
                .matchEquals(ReceiveTimeout.getInstance(), msg -> passivate())
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }

    private void receiveFlowNodeComplete(FlowNodeCompleteMessage completeMessage){

        TaskCompleteEvnt evnt = new TaskCompleteEvnt(completeMessage.data, completeMessage.getNextFlowNodes());
        persist(evnt, (e) -> {
            updateState(e);

            // create task actor for each nextflownode
            // if end event check active tasks list if not more active tasks (Not sure what to do)
            // options: keep instance in memory to get data(passivation) or delete actor
            completeMessage.getNextFlowNodes()
                    .forEach(fnId -> createAndTriggerFlowNodeActor(fnId));
        });
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

    private void getInstanceData(InstanceGetDataMessage message) {
        log.info("Get Instance Data From Instance:"+instanceId+" Data:"+instanceData.toString());
        message.originator.tell(InstanceReturnDataMessage.createMessage(instanceData, message.originator), getSelf());
    }

    private void passivate() {
        log.info("Passivate instance "+instanceId);
        getContext().getParent().tell(
                new ShardRegion.Passivate(PoisonPill.getInstance()), getSelf());
    }
}

class InstanceStartEvnt implements Serializable {
    private static final long serialVersionUID = 1L;
    public final Map data;
    public final long instanceId;
    public final String processModelString;

    public InstanceStartEvnt(long instanceId, Map data, String processModelString) {
        this.instanceId = instanceId;
        this.data = data;
        this.processModelString = processModelString;
    }
}

class TaskCompleteEvnt implements Serializable {
    private static final long serialVersionUID = 1L;
    public final Map data;
    public final ArrayList<String> nextFlowNodes;

    public TaskCompleteEvnt(Map data, ArrayList<String> nextFlowNodes) {
        this.data = data;
        this.nextFlowNodes = nextFlowNodes;
    }
}
