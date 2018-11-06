package com.pintailai.flownode;

import akka.actor.Props;
import com.pintailai.messages.FlowNodeCompleteMessage;
import com.pintailai.messages.FlowNodeStartMessage;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;

import java.lang.reflect.Method;
import java.util.Map;

public class InternalTaskInstance extends AbstractFlowNodeInstance {

    public static Props props(FlowNode fn, BpmnModelInstance process, String taskId) {
        return Props.create(InternalTaskInstance.class, () -> new InternalTaskInstance(fn, process, taskId));
    }

    protected InternalTaskInstance(FlowNode fn, BpmnModelInstance process, String taskId) throws Exception {
        super(fn, process, taskId);
    }

    @Override
    public final Receive createReceive() {
        return receiveBuilder()
                .match(FlowNodeStartMessage.class, (startMessage) -> {
                    log.info("Starting Task with input data: "+startMessage.data.toString()+" For class:"+className);

                    Map outputData = startMessage.data;
                    if(className != "" && className!=null) {
                        try {
                            Class c = Class.forName(className);

                            Method method = c.getMethod("execute", Map.class);
                            outputData = (Map) method.invoke(c.newInstance(), startMessage.data);
                            log.info("Task Completed with output Data: " + outputData.toString());
                        } catch (ClassNotFoundException e) {
                            log.error("Class Not found error: {}", e);
                        } catch (NoSuchMethodException e) {
                            log.error("No Such Mehtod Error: {}", e);
                        } catch (Error e) {
                            log.error("Error: {}", e);
                        }
                    }



                    getSender().tell(FlowNodeCompleteMessage
                            .createMessage(outputData,identifyNextFlowNodes()),getSelf());

                })
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }
}