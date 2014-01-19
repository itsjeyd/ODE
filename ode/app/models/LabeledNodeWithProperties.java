package models;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.F.Function;
import play.libs.F.Promise;

import managers.LabeledNodeWithPropertiesManager;


public abstract class LabeledNodeWithProperties extends LabeledNode {
    public ObjectNode jsonProperties;

    public Promise<Boolean> exists() {
        Promise<JsonNode> json = LabeledNodeWithPropertiesManager.get(this);
        return json.map(new ExistsFunction());
    }


    private class ExistsFunction implements Function<JsonNode, Boolean> {
        public Boolean apply(JsonNode json) {
            return json.get("data").size() > 0;
        }
    }

}
