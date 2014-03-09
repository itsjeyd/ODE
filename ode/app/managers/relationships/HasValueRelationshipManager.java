package managers.relationships;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import play.libs.WS;
import play.libs.F.Function;
import play.libs.F.Promise;

import constants.RelationshipType;
import neo4play.Neo4jService;
import managers.functions.JsonFunction;
import models.nodes.Feature;
import models.nodes.Rule;


public class HasValueRelationshipManager extends HasRelationshipManager {

    public static Promise<JsonNode> getEndNode(
        final Feature startNode, Rule rule) {
        Promise<UUID> ruleUUID = rule.getUUID();
        Promise<WS.Response> response = ruleUUID.flatMap(
            new Function<UUID, Promise<WS.Response>>() {
                public Promise<WS.Response> apply(UUID ruleUUID) {
                    ObjectNode relProps = Json.newObject();
                    relProps.put("rule", ruleUUID.toString());
                    return Neo4jService.getRelationshipTarget(
                        startNode, RelationshipType.HAS, relProps);
                }
            });
        return response.map(new JsonFunction());
    }

}
