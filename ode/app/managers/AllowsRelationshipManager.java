package managers;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.WS;
import play.libs.F.Promise;

import managers.functions.JsonFunction;
import managers.functions.RelationshipCreatedFunction;
import models.AllowsRelationship;
import neo4play.Neo4jService;


public class AllowsRelationshipManager {

    public static Promise<JsonNode> get(AllowsRelationship relationship) {
        Promise<WS.Response> response = Neo4jService.getTypedRelationship(
            relationship);
        return response.map(new JsonFunction());
    }

    public static Promise<Boolean> create(
        AllowsRelationship relationship) {
        Promise<WS.Response> response = Neo4jService
            .createTypedRelationship(
                relationship.startNode, relationship.endNode,
                relationship.type);
        return response.map(new RelationshipCreatedFunction());
    }

}
