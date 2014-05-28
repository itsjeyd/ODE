package managers.relationships;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import play.libs.WS;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.mvc.Http.Status;

import constants.RelationshipType;
import neo4play.Neo4jService;
import models.nodes.AVM;
import models.nodes.Feature;


public class HasFeatureRelationshipManager extends HasRelationshipManager {

    public static Promise<List<JsonNode>> getEndNodes(final AVM startNode) {
        Promise<UUID> uuid = startNode.getUUID();
        Promise<List<WS.Response>> responses = uuid.flatMap(
            new Function<UUID, Promise<List<WS.Response>>>() {
                public Promise<List<WS.Response>> apply(UUID uuid) {
                    ObjectNode jsonProperties = Json.newObject();
                    jsonProperties.put("uuid", uuid.toString());
                    return Neo4jService
                    .getRelationshipTargets(startNode.getLabel(),
                                            jsonProperties,
                                            RelationshipType.HAS.toString());
                }
            });
        return responses.map(
            new Function<List<WS.Response>, List<JsonNode>>() {
                public List<JsonNode> apply(List<WS.Response> responses) {
                    List<JsonNode> nodes = new ArrayList<JsonNode>();
                    for (WS.Response response: responses) {
                        JsonNode json = response.asJson();
                        nodes.add(json.findValue("data"));
                    }
                    return nodes;
                }
            });
    }

}
