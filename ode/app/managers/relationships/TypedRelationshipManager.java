package managers.relationships;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.WS;
import play.libs.F.Promise;

import neo4play.Neo4jService;
import managers.functions.JsonFunction;
import managers.functions.RelationshipCreatedFunction;
import models.relationships.TypedRelationship;


public class TypedRelationshipManager {

    public static Promise<JsonNode> get(TypedRelationship relationship) {
        Promise<WS.Response> response = Neo4jService.getTypedRelationship(
            relationship.startNode, relationship.endNode, relationship.type);
        return response.map(new JsonFunction());
    }

}
