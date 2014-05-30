package models.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import constants.NodeType;


public abstract class AVM extends LabeledNodeWithProperties {
    public Rule rule;
    public JsonNode json;

    protected AVM() {
        super(NodeType.AVM);
    }

    public AVM(Rule rule) {
        this();
        this.rule = rule;
    }

}
