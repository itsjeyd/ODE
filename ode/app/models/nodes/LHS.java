package models.nodes;

import managers.nodes.LHSManager;


public class LHS extends AVM {

    public static final LHSManager nodes = new LHSManager();

    public Rule parent;

    public LHS(String uuid) {
        this.jsonProperties.put("uuid", uuid);
    }

    public LHS(Rule rule) {
        super(rule);
        this.parent = rule;
    }

}
