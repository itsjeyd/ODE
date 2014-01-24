package managers;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.WS;
import play.libs.F.Promise;

import managers.functions.DeletedFunction;
import managers.functions.JsonFunction;
import models.LabeledNodeWithProperties;
import models.Relationship;
import neo4play.Neo4jService;


public class RelationshipManager {

    public static Promise<JsonNode> getAllTo(
        LabeledNodeWithProperties endNode) {
        Promise<WS.Response> response = Neo4jService
            .getIncomingRelationships(endNode.label.toString(),
                                      endNode.jsonProperties);
        return response.map(new JsonFunction());
    }

    public static Promise<Boolean> delete(Relationship relationship) {
        Promise<WS.Response> response = Neo4jService.deleteRelationship(
            relationship);
        return response.map(new DeletedFunction());
    }

}
