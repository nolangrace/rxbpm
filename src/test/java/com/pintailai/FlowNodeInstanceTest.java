package com.pintailai;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.pintailai.flownode.InternalTaskInstance;
import com.pintailai.flownode.TaskInterface;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class FlowNodeInstanceTest {
    public static void main (String[] args){
        final ActorSystem system = ActorSystem.create("bpm-engine-akka-test");
        try {
            Map inputData = new HashMap<String, Object>();
            inputData.put("One", 1);
            inputData.put("Two", 2);

            File bpmnModel = new File("./src/main/resources/bpm-simple.bpmn");
            BpmnModelInstance modelInstance = Bpmn.readModelFromFile(bpmnModel);
            FlowNode fn = modelInstance.getModelElementById("Test_Task");

            ActorRef taskActor = system.actorOf(InternalTaskInstance.props(fn, modelInstance, "1"), "Task-Actor");
            taskActor.tell(inputData, ActorRef.noSender());
        } finally {
            system.terminate();
        }
    }
}