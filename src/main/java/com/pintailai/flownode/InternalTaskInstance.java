package com.pintailai.flownode;

import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.pintailai.messages.FlowNodeCompleteMessage;
import com.pintailai.messages.FlowNodeStartMessage;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
                    log.info("Starting Task with input data: "+startMessage.getData().toString()+" For class:"+className);

//                    try {
//                        Class c = Class.forName(className);
//
//                        Method method = c.getMethod("execute", Map.class);
//                        Map outputData = (Map)method.invoke(c.newInstance(), startMessage.getData());
//                        log.info("Task Completed with output Data: "+outputData.toString());
//                    } catch (ClassNotFoundException e) {
//                        log.error("Class Not found error: {}",e);
//                    } catch (NoSuchMethodException e){
//                        log.error("No Such Mehtod Error: {}",e);
//                    } catch (Error e) {
//                        log.error("Error: {}",e);
//                    }

                    getSender().tell(FlowNodeCompleteMessage
                            .createMessage(startMessage.getData(),identifyNextFlowNode()),getSelf());

                })
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }
}