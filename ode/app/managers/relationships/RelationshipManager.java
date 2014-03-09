package managers.relationships;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.WS;
import play.libs.F.Promise;

import neo4play.Neo4jService;
import managers.functions.RelationshipDeletedFunction;
import managers.functions.JsonFunction;
import models.nodes.LabeledNodeWithProperties;
import models.relationships.Relationship;


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
        return response.map(new RelationshipDeletedFunction());
    }

}
