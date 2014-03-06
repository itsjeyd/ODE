package managers;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import play.libs.WS;
import play.libs.F.Function;
import play.libs.F.Promise;

import constants.RelationshipType;
import models.LHS;
import neo4play.Neo4jService;
import managers.functions.JsonFunction;


public class LHSManager extends LabeledNodeWithPropertiesManager {

    public static Promise<JsonNode> get(LHS lhs) {
        Promise<UUID> ruleUUID = lhs.rule.getUUID();
        Promise<WS.Response> response = ruleUUID.flatMap(
            new Function<UUID, Promise<WS.Response>>() {
                public Promise<WS.Response> apply(UUID ruleUUID) {
                    ObjectNode relProps = Json.newObject();
                    relProps.put("rule", ruleUUID.toString());
                    return Neo4jService.getPath(
                        RelationshipType.HAS, relProps);
                }
            });
        return response.map(new JsonFunction());
    }

}
