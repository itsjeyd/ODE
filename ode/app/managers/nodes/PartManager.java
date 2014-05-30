package managers.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import models.nodes.Part;
import play.libs.F.Function;
import play.libs.F.Promise;


public class PartManager extends ContentNodeManager {

    public PartManager() {
        this.label = "Part";
    }

    // READ

    public Promise<List<Part>> all() {
        Promise<List<JsonNode>> json = all(this.label);
        return json.map(
            new Function<List<JsonNode>, List<Part>>() {
                public List<Part> apply(List<JsonNode> json) {
                    List<Part> parts = new ArrayList<Part>();
                    for (JsonNode node: json) {
                        String content = node.get("content").asText();
                        parts.add(new Part(content));
                    }
                    return parts;
                }
            });
    }

}
