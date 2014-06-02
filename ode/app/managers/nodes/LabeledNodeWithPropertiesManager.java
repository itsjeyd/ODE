package managers.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import models.nodes.Node;
import play.libs.F.Promise;


abstract class LabeledNodeWithPropertiesManager extends LabeledNodeManager {

    public Promise<? extends Node> get(JsonNode properties) {
        return null;
    }

    protected Promise<Boolean> connect(
        JsonNode startNode, JsonNode endNode, String location) {
        return null;
    }

    protected Promise<Boolean> disconnect(
        JsonNode startNode, JsonNode endNode, String location) {
        return null;
    }

}
