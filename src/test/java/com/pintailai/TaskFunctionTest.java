package com.pintailai;

import com.pintailai.flownode.TaskInterface;

import java.util.Map;

public class TaskFunctionTest implements TaskInterface {

    public Map execute(Map inputData){
        System.out.println("Within Task FunctionTest");
        return inputData;
    }

}