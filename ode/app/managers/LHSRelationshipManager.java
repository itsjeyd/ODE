package managers;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.WS;
import play.libs.F.Promise;

import models.LHSRelationship;
import neo4play.Neo4jService;
import managers.functions.JsonFunction;
import managers.functions.RelationshipCreatedFunction;


public class LHSRelationshipManager {

    public static Promise<JsonNode> get(LHSRelationship relationship) {
        Promise<WS.Response> response = Neo4jService.getTypedRelationship(
            relationship.startNode, relationship.endNode, relationship.type);
        return response.map(new JsonFunction());
    }

    public static Promise<Boolean> create(LHSRelationship relationship) {
        Promise<WS.Response> response = Neo4jService
            .createTypedRelationship(
                relationship.startNode, relationship.endNode,
                relationship.type);
        return response.map(new RelationshipCreatedFunction());
    }

}
