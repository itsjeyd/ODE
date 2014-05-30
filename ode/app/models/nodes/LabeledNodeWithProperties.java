package models.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import constants.NodeType;
import play.libs.Json;


public abstract class LabeledNodeWithProperties extends LabeledNode {

    public ObjectNode jsonProperties;

    protected LabeledNodeWithProperties(NodeType label) {
        this.label = label;
        this.jsonProperties = Json.newObject();
    }

    public JsonNode getProperties() {
        return this.jsonProperties;
    }

}
