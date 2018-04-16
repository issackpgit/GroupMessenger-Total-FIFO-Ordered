package edu.buffalo.cse.cse486586.groupmessenger2;

import java.io.Serializable;

/**
 * Created by issackoshypanicker on 3/2/18.
 */

public class ProposalMessage implements Serializable {

    String id;
    int propPriority;

    public ProposalMessage(String id, int proposedNum){
        this.id = id;
        this.propPriority = proposedNum;
    }
}
