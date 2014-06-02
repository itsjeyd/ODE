package managers.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import play.libs.F.Promise;


abstract class LabeledNodeWithPropertiesManager extends LabeledNodeManager {

    protected Promise<Boolean> connect(
        JsonNode startNode, JsonNode endNode, String location) {
        return Promise.pure(false);
    }

    protected Promise<Boolean> disconnect(
        JsonNode startNode, JsonNode endNode, String location) {
        return Promise.pure(false);
    }

}
