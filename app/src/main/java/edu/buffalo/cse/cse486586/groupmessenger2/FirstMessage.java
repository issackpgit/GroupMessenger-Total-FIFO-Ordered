package edu.buffalo.cse.cse486586.groupmessenger2;

import java.io.Serializable;

/**
 * Created by issackoshypanicker on 3/2/18.
 */

public class FirstMessage implements Serializable{
    String id;
    String msg;
    int counter;

    public FirstMessage(String msg, int counter, String id){
        this.msg = msg;
        this.counter = counter;
        this.id = id;
    }
}
