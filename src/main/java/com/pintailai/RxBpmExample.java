package com.pintailai;

import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.remote.artery.aeron.TaskRunner;
import com.pintailai.process.RxBpm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RxBpmExample {
    public static void main(String[] args){

        try {

//            Map inputData = new HashMap<String, Object>();
//            inputData.put("One", 1);
//            inputData.put("Two", 2);
            //initialize engine w/ one task diagram

            List seedNodes = new ArrayList<Address>();
//            seedNodes.add(new Address("akka.tcp", "RXBPM", "localhost", 2554));

            String[] ports = new String[] { "2551" , "2552"};


            RxBpm rxbpm = new RxBpm("bpm-simple.bpmn", seedNodes, ports);


//            rxbpm.createInstance("StartEvent_1", inputData);

            //create instance

            //check instance status

            //validate outputs
        }finally {
        }
    }
}
