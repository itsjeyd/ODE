package managers.relationships;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import play.libs.WS;
import play.libs.F.Function;
import play.libs.F.Promise;

import neo4play.Neo4jService;
import managers.functions.JsonFunction;
import managers.functions.RelationshipCreatedFunction;
import models.relationships.HasFeatureRelationship;


public class HasFeatureRelationshipManager {

    public static Promise<JsonNode> get(
        HasFeatureRelationship relationship) {
        Promise<WS.Response> response = Neo4jService.getTypedRelationship(
            relationship.startNode, relationship.endNode, relationship.type);
        return response.map(new JsonFunction());
    }

    public static Promise<Boolean> create(
        final HasFeatureRelationship relationship) {
        Promise<UUID> ruleUUID = relationship.startNode.rule.getUUID();
        Promise<WS.Response> response = ruleUUID.flatMap(
            new Function<UUID, Promise<WS.Response>>() {
                public Promise<WS.Response> apply(UUID ruleUUID) {
                    ObjectNode data = Json.newObject();
                    data.put("rule", ruleUUID.toString());
                    return Neo4jService
                        .createTypedRelationshipWithProperties(
                            relationship.startNode, relationship.endNode,
                            relationship.type, data);
                }
            });
        return response.map(new RelationshipCreatedFunction());
    }

}
