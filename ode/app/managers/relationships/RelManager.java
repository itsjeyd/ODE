package managers.relationships;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import managers.BaseManager;
import models.nodes.LabeledNodeWithProperties;
import neo4play.RelationshipService;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.WS;


public abstract class RelManager extends BaseManager {

    public Promise<List<JsonNode>> to(LabeledNodeWithProperties endNode) {
        Promise<WS.Response> response = RelationshipService.to(endNode);
        return response.map(
            new Function<WS.Response, List<JsonNode>>() {
                public List<JsonNode> apply(WS.Response response) {
                    JsonNode json = response.asJson();
                    return json.get("data").findValues("data");
                }
            });
    }

}
