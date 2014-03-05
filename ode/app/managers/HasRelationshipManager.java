package managers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import play.libs.WS;
import play.libs.F.Promise;

import models.HasRelationship;
import neo4play.Neo4jService;
import managers.functions.JsonFunction;
import managers.functions.RelationshipCreatedFunction;


public class HasRelationshipManager {

    public static Promise<JsonNode> get(HasRelationship relationship) {
        Promise<WS.Response> response = Neo4jService.getTypedRelationship(
            relationship.startNode, relationship.endNode, relationship.type);
        return response.map(new JsonFunction());
    }

    public static Promise<Boolean> create(HasRelationship relationship) {
        ObjectNode data = Json.newObject();
        data.put("rule", relationship.startNode.rule.name);
        Promise<WS.Response> response = Neo4jService
            .createTypedRelationshipWithProperties(
                relationship.startNode, relationship.endNode,
                relationship.type, data);
        return response.map(new RelationshipCreatedFunction());
    }

}
