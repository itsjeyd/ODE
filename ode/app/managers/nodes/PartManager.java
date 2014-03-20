package managers.nodes;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.F.Promise;

import constants.NodeType;


public class PartManager extends LabeledNodeWithPropertiesManager {

    public static Promise<List<JsonNode>> all() {
        return LabeledNodeManager.all(NodeType.PART);
    }

}
