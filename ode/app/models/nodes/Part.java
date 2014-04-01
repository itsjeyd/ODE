package models.nodes;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.F.Function;
import play.libs.F.Promise;

import constants.NodeType;
import managers.nodes.PartManager;

public class Part extends LabeledNodeWithProperties {
    public String content;

    private Part(String content) {
        super(NodeType.PART);
        this.content = content;
    }

    public static Promise<List<Part>> all() {
        Promise<List<JsonNode>> json = PartManager.all();
        Promise<List<Part>> parts = json.map(new AllFunction());
        return parts;
    }

    private static class AllFunction
        implements Function<List<JsonNode>, List<Part>> {
        public List<Part> apply(List<JsonNode> dataNodes) {
            List<Part> parts = new ArrayList<Part>();
            for (JsonNode dataNode: dataNodes) {
                String content = dataNode.get("content").asText();
                parts.add(new Part(content));
            }
            return parts;
        }
    }

}
