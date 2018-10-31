package com.pintailai.process;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.pintailai.flownode.InternalTaskInstance;
import com.pintailai.messages.InstanceStartMessage;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

//Process Engine manages creating instances, looking up instance data, and suspending/terminating instances and tasks
public class ProcessEngine extends AbstractActor {
    protected final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    protected final ActorSystem system = getContext().getSystem();
    BpmnModelInstance process;

    public static Props props(BpmnModelInstance process) {
        return Props.create(ProcessEngine.class, () -> new ProcessEngine(process));
    }

    private ProcessEngine(BpmnModelInstance process) {
        this.process = process;
    }

    @Override
    public final Receive createReceive() {
        return receiveBuilder()
                .match(InstanceStartMessage.class, (instanceStartMessage) -> {
                    UUID uuid = UUID.randomUUID();
                    String newInstaceId = uuid.toString();

                    // create new instance Actor
                    ActorRef instanceActor = system.actorOf(com.pintailai.process.ProcessInstance.
                                    props(process, newInstaceId), "InstanceActor-"+newInstaceId);

                    // Send message to instance actor to start process
                    instanceActor.tell(instanceStartMessage, getSelf());
                })
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }
}
