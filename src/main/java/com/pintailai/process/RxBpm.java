package com.pintailai.process;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.cluster.Cluster;
import com.pintailai.messages.InstanceStartMessage;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.StartEvent;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class RxBpm {
    private BpmnModelInstance model;
    private ActorRef processEngineActor;
    private ActorSystem system;
    private Cluster cluster;
    private List<Address> seedNodes;
    private int port;
    private String hostname;

    public RxBpm(String bpmFileName){
        InputStream bpmnStream = getClass().getResourceAsStream("/"+bpmFileName);

        this.model = Bpmn.readModelFromStream(bpmnStream);

        getConfigurations();

        start();
    }

    // Create Process Engine Actor
    private void start(){
        // Override the configuration of the port when specified as program argument
        final Config config =
                ConfigFactory.parseString(
                        "akka.remote.netty.tcp.port=" + port + "\n" +
                                "akka.remote.artery.canonical.port=" + port+ "\n" +
                                    "akka.remote.netty.tcp.hostname=" + hostname+ "\n" +
                                        "akka.remote.artery.canonical.hostname=" + hostname)
//                        .withFallback(ConfigFactory.parseString("akka.cluster.roles = [backend]"))
                        .withFallback(ConfigFactory.load());

        system = ActorSystem.create("RXBPM", config);
//        processEngineActor = system.actorOf(com.pintailai.process.ProcessEngine.props(model), "ProcessEngineActor");

        cluster = Cluster.get(system);

        if(seedNodes!= null && seedNodes.size() != 0) {
            cluster.joinSeedNodes(seedNodes);
        }
    }

    public void createInstance(){
        processEngineActor.tell("Create Process", ActorRef.noSender());
    }

    public void createInstance(String startEventId){
        StartEvent startEvent = model.getModelElementById(startEventId);

        InstanceStartMessage.createMessage(null, model, startEvent);

        processEngineActor.tell(InstanceStartMessage.createMessage(null, model, startEvent)
                , ActorRef.noSender());
    }

    public void createInstance(String startEventId, Map inputData){
        StartEvent startEvent = model.getModelElementById(startEventId);

        processEngineActor.tell(InstanceStartMessage.createMessage(inputData, model, startEvent)
                , ActorRef.noSender());
    }

    private void getConfigurations(){
        if(System.getenv("BPMRX_PORT")!="" && System.getenv("BPMRX_PORT")!=null)
            port = Integer.parseInt(System.getenv("BPMRX_PORT"));
        else
            port = 2554;

        System.out.println("Cluster port set to "+port);

        System.out.println("RXBPM_SEED_NODES: "+System.getenv("RXBPM_SEED_NODES"));

        if(System.getenv("RXBPM_SEED_NODES")!="" && System.getenv("RXBPM_SEED_NODES")!=null) {
            String[] seedNodeArray = System.getenv("RXBPM_SEED_NODES").split(",");
            seedNodes = Arrays.stream(seedNodeArray).map(seedAddress -> {
                return new Address("akka.tcp", "RXBPM", seedAddress, port);
            }).collect(Collectors.toList());

            System.out.println("Seed Nodes Found "+System.getenv("RXBPM_SEED_NODES"));
        }
        else{
            seedNodes = new ArrayList<>();

            System.out.println("No Seed Nodes Found");
        }

        if(System.getenv("HOSTNAME")!="" && System.getenv("HOSTNAME")!=null){
            hostname = System.getenv("HOSTNAME");
        } else {
            hostname = "localhost";
        }
    }


}
