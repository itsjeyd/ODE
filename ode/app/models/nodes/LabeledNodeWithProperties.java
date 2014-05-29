package models.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import constants.NodeType;
import managers.nodes.LabeledNodeWithPropertiesManager;
import models.functions.ExistsFunction;
import play.libs.F.Promise;
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

    public Promise<Boolean> exists() {
        Promise<JsonNode> json = LabeledNodeWithPropertiesManager.get(this);
        return json.map(new ExistsFunction());
    }

}
