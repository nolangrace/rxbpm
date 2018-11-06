package com.pintailai;

import com.pintailai.flownode.TaskInterface;

import java.util.Map;

public class TaskFunctionTest implements TaskInterface {

    public Map execute(Map inputData){
        System.out.println("Within Task FunctionTest");

        inputData.put("test",(int)inputData.get("test")+1);

        return inputData;
    }

}