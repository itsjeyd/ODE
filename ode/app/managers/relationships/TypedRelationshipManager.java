package managers.relationships;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.WS;
import play.libs.F.Promise;

import constants.RelationshipType;
import neo4play.Neo4jService;
import managers.functions.JsonFunction;
import managers.functions.RelationshipCreatedFunction;
import models.nodes.Feature;
import models.relationships.TypedRelationship;


public class TypedRelationshipManager {

    public static Promise<JsonNode> get(TypedRelationship relationship) {
        Promise<WS.Response> response = Neo4jService.getTypedRelationship(
            relationship.startNode, relationship.endNode, relationship.type);
        return response.map(new JsonFunction());
    }

    public static Promise<Boolean> create(TypedRelationship relationship) {
        Promise<WS.Response> response = Neo4jService
            .createTypedRelationship(
                relationship.startNode, relationship.endNode,
                relationship.type);
        return response.map(new RelationshipCreatedFunction());
    }

    public static Promise<JsonNode> getAllFrom(
        Feature startNode, RelationshipType type) {
        Promise<WS.Response> response = Neo4jService
            .getOutgoingRelationshipsByType(startNode.getLabel(),
                                            startNode.jsonProperties,
                                            type.name());
        return response.map(new JsonFunction());
    }

}
