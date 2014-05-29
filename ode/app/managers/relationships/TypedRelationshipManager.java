package managers.relationships;

import com.fasterxml.jackson.databind.JsonNode;
import managers.functions.JsonFunction;
import models.relationships.TypedRelationship;
import neo4play.Neo4jService;
import play.libs.F.Promise;
import play.libs.WS;


public class TypedRelationshipManager {

    public static Promise<JsonNode> get(TypedRelationship relationship) {
        Promise<WS.Response> response = Neo4jService.getTypedRelationship(
            relationship.startNode, relationship.endNode, relationship.type);
        return response.map(new JsonFunction());
    }

}
