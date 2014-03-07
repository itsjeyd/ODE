package managers.relationships;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.WS;
import play.libs.F.Promise;

import constants.RelationshipType;
import neo4play.Neo4jService;
import managers.functions.JsonFunction;
import models.nodes.Feature;


public class TypedRelationshipManager {

    public static Promise<JsonNode> getAllFrom(
        Feature startNode, RelationshipType type) {
        Promise<WS.Response> response = Neo4jService
            .getOutgoingRelationshipsByType(startNode.label.toString(),
                                            startNode.jsonProperties,
                                            type.name());
        return response.map(new JsonFunction());
    }

}
