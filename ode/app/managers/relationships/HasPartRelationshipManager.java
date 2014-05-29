package managers.relationships;

import com.fasterxml.jackson.databind.JsonNode;
import constants.RelationshipType;
import java.util.ArrayList;
import java.util.List;
import models.nodes.Slot;
import neo4play.Neo4jService;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.WS;


public class HasPartRelationshipManager extends TypedRelationshipManager {

    public static Promise<List<JsonNode>> getEndNodes(final Slot startNode) {
        Promise<List<WS.Response>> responses = Neo4jService
            .getRelationshipTargets(startNode.getLabel(),
                                    startNode.jsonProperties,
                                    RelationshipType.HAS.toString());
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
