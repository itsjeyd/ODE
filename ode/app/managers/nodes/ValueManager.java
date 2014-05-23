package managers.nodes;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.WS;
import play.libs.F.Promise;

import constants.NodeType;
import constants.RelationshipType;
import neo4play.Neo4jService;
import managers.functions.JsonFunction;
import models.nodes.Value;


public class ValueManager extends NamedNodeManager {

    public static Promise<List<JsonNode>> staticAll() {
        return LabeledNodeManager.all(NodeType.VALUE);
    }

    public static Promise<JsonNode> getIncomingRelationships(Value value) {
        Promise<WS.Response> response = Neo4jService
            .getIncomingRelationshipsByType(value.getLabel(),
                                            value.jsonProperties,
                                            RelationshipType.ALLOWS.name());
        return response.map(new JsonFunction());
    }

}
