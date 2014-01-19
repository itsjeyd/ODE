package managers;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.WS;
import play.libs.F.Promise;

import constants.RelationshipType;
import managers.functions.JsonFunction;
import models.Feature;
import neo4play.Neo4jService;


public class TypedRelationshipManager {

    private static Neo4jService dbService = new Neo4jService();

    public static Promise<JsonNode> getAllFrom(
        Feature startNode, RelationshipType type) {
        Promise<WS.Response> response = dbService
            .getOutgoingRelationshipsByType(startNode, type);
        return response.map(new JsonFunction());
    }

}
