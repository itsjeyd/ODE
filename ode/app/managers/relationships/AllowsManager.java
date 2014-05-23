package managers.relationships;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import managers.relationships.AllowsManager;
import models.nodes.LabeledNodeWithProperties;
import neo4play.RelationshipService;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.WS;


public class AllowsManager extends RelManager {

    public AllowsManager() {
        this.type = "ALLOWS";
    }

    @Override
    public Promise<List<JsonNode>> to(LabeledNodeWithProperties endNode) {
        Promise<WS.Response> response = RelationshipService
            .to(endNode, this.type);
        return response.map(
            new Function<WS.Response, List<JsonNode>>() {
                public List<JsonNode> apply(WS.Response response) {
                    JsonNode json = response.asJson();
                    return json.get("data").findValues("data");
                }
            });
    }

}
