package managers;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.F.Promise;

import constants.NodeType;
import models.Value;


public class ValueManager extends LabeledNodeWithPropertiesManager {

    public static Promise<List<JsonNode>> all() {
        return LabeledNodeManager.all(NodeType.VALUE);
    }

    public static Promise<Boolean> create(Value value) {
        return LabeledNodeWithPropertiesManager.create(value);
    }

}
