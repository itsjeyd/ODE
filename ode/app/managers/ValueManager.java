package managers;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.WS;
import play.libs.F.Promise;

import constants.NodeType;
import constants.RelationshipType;
import managers.functions.JsonFunction;
import managers.functions.NodeDeletedFunction;
import models.Value;
import neo4play.Neo4jService;


public class ValueManager extends LabeledNodeWithPropertiesManager {

    public static Promise<List<JsonNode>> all() {
        return LabeledNodeManager.all(NodeType.VALUE);
    }

    public static Promise<Boolean> create(Value value) {
        return LabeledNodeWithPropertiesManager.create(value);
    }

    public static Promise<JsonNode> getIncomingRelationships(Value value) {
        Promise<WS.Response> response = Neo4jService
            .getIncomingRelationshipsByType(value.label.toString(),
                                            value.jsonProperties,
                                            RelationshipType.ALLOWS.name());
        return response.map(new JsonFunction());
    }

    public static Promise<Boolean> delete(Value value) {
        Promise<WS.Response> response = Neo4jService
            .deleteLabeledNodeWithProperties(value.label.toString(),
                                             value.jsonProperties);
        return response.map(new NodeDeletedFunction());
    }

}
