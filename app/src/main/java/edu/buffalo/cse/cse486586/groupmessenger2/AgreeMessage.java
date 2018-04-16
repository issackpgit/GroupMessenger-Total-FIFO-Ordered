package edu.buffalo.cse.cse486586.groupmessenger2;

import java.io.Serializable;

/**
 * Created by issackoshypanicker on 3/2/18.
 */

public class AgreeMessage implements Serializable {
    String id;
    int maxPriority;

    public AgreeMessage() {
        id = "";
        maxPriority = 0;
    }

    public AgreeMessage(String id, int maxPriority){
        this.id = id;
        this.maxPriority = maxPriority;
    }

}
