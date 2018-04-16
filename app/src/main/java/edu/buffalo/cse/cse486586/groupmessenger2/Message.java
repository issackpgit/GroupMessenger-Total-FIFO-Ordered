package edu.buffalo.cse.cse486586.groupmessenger2;

/**
 * Created by issackoshypanicker on 3/2/18.
 */

public class Message  {
    String msg;
    int priority;
    String id;
    boolean deliverable;

    public Message(){
        msg = "";
        priority = 0;
        id = "";
        deliverable = false;
    }

    public int getPortNo() {
        String[] temp = id.split("-");
        return Integer.parseInt(temp[0]);
    }

    public int getCounter() {
        String[] temp = id.split("-");
        return Integer.parseInt(temp[1]);
    }
}
