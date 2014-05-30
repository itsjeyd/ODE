package managers.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import models.nodes.LHS;
import play.libs.F.Function;
import play.libs.F.Promise;


public class LHSManager extends AVMManager {

    // READ

    public Promise<LHS> get(JsonNode properties) {
        final LHS lhs = new LHS(properties.get("uuid").asText());
        Promise<JsonNode> json = toJSON(properties);
        return json.map(
            new Function<JsonNode, LHS>() {
                public LHS apply(JsonNode json) {
                    lhs.json = json;
                    return lhs;
                }
            });
    }

}
