package models.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.F.Promise;

import managers.nodes.LabeledNodeWithPropertiesManager;
import models.functions.ExistsFunction;


public abstract class LabeledNodeWithProperties extends LabeledNode {
    public ObjectNode jsonProperties;

    public Promise<Boolean> exists() {
        Promise<JsonNode> json = LabeledNodeWithPropertiesManager.get(this);
        return json.map(new ExistsFunction());
    }

}
