package managers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import play.libs.WS;
import play.libs.F.Promise;

import constants.RelationshipType;
import models.LHS;
import neo4play.Neo4jService;
import managers.functions.JsonFunction;


public class LHSManager extends LabeledNodeWithPropertiesManager {

    public static Promise<JsonNode> get(LHS lhs) {
        ObjectNode relProps = Json.newObject();
        relProps.put("rule", lhs.rule.name);
        Promise<WS.Response> response = Neo4jService
            .getPath(RelationshipType.HAS, relProps);
        return response.map(new JsonFunction());
    }

}
