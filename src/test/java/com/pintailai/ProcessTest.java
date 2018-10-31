package com.pintailai;

import akka.actor.ActorSystem;
import com.pintailai.process.RxBpm;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ProcessTest {
    public static void main(String[] args){

        ActorSystem system = ActorSystem.create("rxbpm");;
        try {

            Map inputData = new HashMap<String, Object>();
            inputData.put("One", 1);
            inputData.put("Two", 2);
            //initialize engine w/ one task diagram
            RxBpm rxbpm = new RxBpm("bpm-simple.bpmn");


            rxbpm.createInstance("StartEvent_1", inputData);

            //create instance

            //check instance status

            //validate outputs
        }finally {
            system.terminate();
        }
    }
}
