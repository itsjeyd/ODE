package managers.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
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

    // Custom functionality

    protected Promise<List<JsonNode>> features(JsonNode properties) {
        Promise<JsonNode> lhs = LHS.nodes.toJSON(properties);
        Promise<List<JsonNode>> features = lhs.map(
            new Function<JsonNode, List<JsonNode>>() {
                public List<JsonNode> apply(JsonNode lhs) {
                    List<JsonNode> features = new ArrayList<JsonNode>();
                    List<JsonNode> nodes = lhs.findValues("attribute");
                    for (JsonNode node: nodes) {
                        features.add(((ObjectNode) node).retain("name"));
                    }
                    return features;
                }
            });
        return features;
    }

}
