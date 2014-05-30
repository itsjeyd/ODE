package models.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import constants.NodeType;
import managers.nodes.RHSManager;


public class RHS extends LabeledNodeWithProperties {

    public static final RHSManager nodes = new RHSManager();

    public Rule rule;
    public JsonNode json;

    private RHS() {
        super(NodeType.RHS);
    }

    public RHS(Rule rule) {
        this();
        this.rule = rule;
    }

    public RHS(String uuid) {
        this();
        this.jsonProperties.put("uuid", uuid);
    }

    public RHS(Rule rule, String uuid) {
        this(rule);
        this.jsonProperties.put("uuid", uuid);
    }

}
