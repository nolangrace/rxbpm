package com.pintailai.messages;

import java.util.Map;

public class InstanceReturnDataMessage extends AbstractMessage {

    public static InstanceReturnDataMessage createMessage(Map data){
        return new InstanceReturnDataMessage(data);
    }

    public InstanceReturnDataMessage(Map data){
        super(data);
    }
}
