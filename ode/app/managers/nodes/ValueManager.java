package managers.nodes;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import play.libs.WS;
import play.libs.F.Promise;

import constants.NodeType;
import constants.RelationshipType;
import neo4play.Neo4jService;
import managers.functions.JsonFunction;
import managers.functions.NodeDeletedFunction;
import managers.functions.UpdatedFunction;
import models.nodes.Value;


public class ValueManager extends LabeledNodeWithPropertiesManager {

    public static Promise<List<JsonNode>> all() {
        return LabeledNodeManager.all(NodeType.VALUE);
    }

    public static Promise<Boolean> create(Value value) {
        return LabeledNodeWithPropertiesManager.create(value);
    }

    public static Promise<JsonNode> getIncomingRelationships(Value value) {
        Promise<WS.Response> response = Neo4jService
            .getIncomingRelationshipsByType(value.getLabel(),
                                            value.jsonProperties,
                                            RelationshipType.ALLOWS.name());
        return response.map(new JsonFunction());
    }

    public static Promise<Boolean> updateName(Value value, String newName) {
        ObjectNode newProps = Json.newObject();
        newProps.put("name", newName);
        Promise<WS.Response> response = Neo4jService.updateNodeProperties(
            value.getLabel(), value.jsonProperties, newProps);
        return response.map(new UpdatedFunction());
    }

}
