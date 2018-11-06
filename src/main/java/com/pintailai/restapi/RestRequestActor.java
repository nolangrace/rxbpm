package com.pintailai.restapi;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.cluster.sharding.ClusterSharding;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import com.pintailai.messages.InstanceGetDataMessage;
import com.pintailai.messages.InstanceReturnDataMessage;
import com.pintailai.messages.InstanceStartMessage;
import com.pintailai.messages.InstanceStartedMessage;
import com.pintailai.processinstance.ProcessInstance;

public class RestRequestActor extends AbstractActor {
    protected final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final ActorSystem system = getContext().getSystem();

    public static Props props() {
        return Props.create(RestRequestActor.class, () -> new RestRequestActor());
    }

    protected RestRequestActor() {
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(InstanceStartMessage.class, (startMessage) -> {
                    log.info("Rest Request Actor Received request to start new Instance");



                    ActorRef instanceRegion = ClusterSharding.get(system).shardRegion("ProcessInstanceRegion");
                    instanceRegion.tell(new ProcessInstance.EntityEnvelope(startMessage.instanceId,
                                    InstanceStartMessage.createMessage(startMessage.instanceId, startMessage.data,
                                            startMessage.startEventId, startMessage.processModelString,
                                            getSender())), getSelf());
                })
                .match(InstanceGetDataMessage.class, (instanceGetDataMessage)->{
                    log.info("Rest Request Actor Recieved request for instance data on instance "+instanceGetDataMessage.instanceId);

                    ActorRef instanceRegion = ClusterSharding.get(system).shardRegion("ProcessInstanceRegion");
                    instanceRegion.tell(new ProcessInstance.EntityEnvelope(instanceGetDataMessage.instanceId,
                            InstanceGetDataMessage.createMessage(instanceGetDataMessage.instanceId, getSender())), getSelf());
                })
//                .match(TaskClaimMessage.class,)
//                .match(TaskGetDataMessage.class,)
//                .match(TaskSetDataMessage.class,)
//                .match(TaskCompleteMessage.class, )
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }


}
