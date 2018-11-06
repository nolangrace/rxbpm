package com.pintailai.process;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.sharding.ClusterSharding;
import akka.cluster.sharding.ClusterShardingSettings;
import akka.cluster.sharding.ShardRegion;

import akka.pattern.Patterns;
import akka.util.Timeout;
import com.pintailai.messages.InstanceStartMessage;
import com.pintailai.processinstance.ProcessInstance;
import com.pintailai.restapi.RestRequestActor;
import com.pintailai.restapi.RxBpmRestApi;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import scala.concurrent.Await;
import scala.concurrent.Future;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RxBpm {
    private BpmnModelInstance model;

    private Cluster cluster;
    private List<Address> seedNodes;
    private String[] ports;
    private String hostname;
    private ActorSystem system;

    public RxBpm(String bpmFileName, List<Address> seednodes, String[] ports){
        InputStream bpmnStream = getClass().getResourceAsStream("/"+bpmFileName);

        this.model = Bpmn.readModelFromStream(bpmnStream);
        this.seedNodes = seednodes;
        this.ports = ports;

//        getConfigurations();

        startEngine();

//        try {
//            RxBpmRestApi.startUp(this);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    // Create Process Engine Actor
    private void startEngine(){
        // Override the configuration of the port when specified as program argument
//        final Config config =
//                ConfigFactory.parseString(
//                        "akka.remote.netty.tcp.port=" + port + "\n" +
//                                "akka.remote.artery.canonical.port=" + port+ "\n" +
//                                    "akka.remote.netty.tcp.hostname=" + hostname+ "\n" +
//                                        "akka.remote.artery.canonical.hostname=" + hostname)
////                        .withFallback(ConfigFactory.parseString("akka.cluster.roles = [backend]"))
//                        .withFallback(ConfigFactory.load());

//        system = ActorSystem.create("RXBPM", config);
//        processEngineActor = system.actorOf(com.pintailai.process.ProcessEngine.props(model), "ProcessEngineActor");

//        cluster = Cluster.get(system);

//        if(seedNodes!= null && seedNodes.size() != 0) {
//            System.out.println("Seed nodes found");
//            cluster.joinSeedNodes(seedNodes);
//        }

        for (String port : ports) {
            // Override the configuration of the port
            Config config = ConfigFactory.parseString(
                    "akka.remote.netty.tcp.port=" + port).withFallback(
                    ConfigFactory.load());

            // Create an Akka system
            system = ActorSystem.create("BPMRX", config);

            // Create an actor that starts the sharding and sends random messages
//            system.actorOf(Props.create(Devices.class));
            ClusterShardingSettings settings = ClusterShardingSettings.create(system);
            ActorRef instanceRegion = ClusterSharding.get(system).start("ProcessInstanceRegion",
                    Props.create(ProcessInstance.class), settings, messageExtractor);

        }

//        Timeout timeout = Timeout.create(Duration.ofSeconds(5));

//
//        Map inputData = new HashMap();
//
//        // create new instance Actor
//        ActorRef requestActor = system.actorOf(RestRequestActor.props()
//                , "RestRequest-"+ UUID.randomUUID().toString());
//
//        requestActor.tell(InstanceStartMessage
//                .createMessage(inputData, "StartEvent_1",
//                        Bpmn.convertToString(model), null), ActorRef.noSender());

        try {
            RxBpmRestApi.startUp(system,this);
        } catch (IOException e) {
            e.printStackTrace();
        }

//        Config config = ConfigFactory.parseString(
//                "akka.remote.netty.tcp.port=" + port).withFallback(
//                ConfigFactory.load());

        // Create an Akka system



    }

    public BpmnModelInstance getModel() {
        return model;
    }

    //    public void createInstance(){
//        processEngineActor.tell("Create Process", ActorRef.noSender());
//    }
//
//    public void createInstance(String startEventId){
//        StartEvent startEvent = model.getModelElementById(startEventId);
//
//        InstanceStartMessage.createMessage(null, startEventId);
//
//        processEngineActor.tell(InstanceStartMessage.createMessage(null, startEventId)
//                , ActorRef.noSender());
//    }

//    public void createInstance(String startEventId, Map inputData){
//        StartEvent startEvent = model.getModelElementById(startEventId);
//
//        processEngineActor.tell(InstanceStartMessage.createMessage(inputData, startEventId)
//                , ActorRef.noSender());
//    }

//    private void getConfigurations(){
//        if(System.getenv("BPMRX_PORT")!="" && System.getenv("BPMRX_PORT")!=null)
//            port = Integer.parseInt(System.getenv("BPMRX_PORT"));
//        else
//            port = 2554;

//        System.out.println("Cluster port set to "+port);
//
//        System.out.println("RXBPM_SEED_NODES: "+System.getenv("RXBPM_SEED_NODES"));
//
//        if(System.getenv("RXBPM_SEED_NODES")!="" && System.getenv("RXBPM_SEED_NODES")!=null
//                && seedNodes==null) {
//            String[] seedNodeArray = System.getenv("RXBPM_SEED_NODES").split(",");
//            seedNodes = Arrays.stream(seedNodeArray).map(seedAddress -> {
//                return new Address("akka.tcp", "RXBPM", seedAddress, port);
//            }).collect(Collectors.toList());
//
//            System.out.println("Seed Nodes Found "+System.getenv("RXBPM_SEED_NODES"));
//        }
//        else if(seedNodes==null){
//            seedNodes = new ArrayList<>();
//
//            System.out.println("No Seed Nodes Found");
//        }
//
//        if(System.getenv("HOSTNAME")!="" && System.getenv("HOSTNAME")!=null){
//            hostname = System.getenv("HOSTNAME");
//        } else {
//            hostname = "localhost";
//        }
//    }

    ShardRegion.MessageExtractor messageExtractor = new ShardRegion.MessageExtractor() {

        @Override
        public String entityId(Object message) {
            if (message instanceof ProcessInstance.EntityEnvelope)
                return String.valueOf(((ProcessInstance.EntityEnvelope) message).instanceId);
            else if (message instanceof ProcessInstance.Get)
                return String.valueOf(((ProcessInstance.Get) message).instanceId);
            else
                return null;
        }

        @Override
        public Object entityMessage(Object message) {
            if (message instanceof ProcessInstance.EntityEnvelope)
                return ((ProcessInstance.EntityEnvelope) message).payload;
            else
                return message;
        }

        @Override
        public String shardId(Object message) {
            int numberOfShards = 100;
            if (message instanceof ProcessInstance.EntityEnvelope) {
                long id = ((ProcessInstance.EntityEnvelope) message).instanceId;
                return String.valueOf(id % numberOfShards);
            } else if (message instanceof ProcessInstance.Get) {
                long id = ((ProcessInstance.Get) message).instanceId;
                return String.valueOf(id % numberOfShards);
            } else {
                return null;
            }
        }

    };


}
