package com.pintailai.messages;

import java.io.Serializable;
import java.util.Map;

public class AbstractMessage implements Serializable {
    public final Map data;

    protected AbstractMessage(Map data){
        this.data = data;
    }
}
