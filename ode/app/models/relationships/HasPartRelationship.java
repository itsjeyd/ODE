package models.relationships;

import com.fasterxml.jackson.databind.JsonNode;
import constants.RelationshipType;
import java.util.ArrayList;
import java.util.List;
import managers.relationships.HasPartRelationshipManager;
import models.nodes.Part;
import models.nodes.Slot;
import play.libs.F.Function;
import play.libs.F.Promise;


public class HasPartRelationship extends TypedRelationship {

    public HasPartRelationship(Slot startNode, Part endNode) {
        super(RelationshipType.HAS, startNode, endNode);
    }

    public static Promise<List<Part>> getEndNodes(final Slot startNode) {
        Promise<List<JsonNode>> endNodes = HasPartRelationshipManager
            .getEndNodes(startNode);
        return endNodes.map(
            new Function<List<JsonNode>, List<Part>>() {
                public List<Part> apply(List<JsonNode> partNodes) {
                    List<Part> parts = new ArrayList<Part>();
                    for (JsonNode partNode: partNodes) {
                        if (!partNode.has("name")) {
                            String uuid = partNode.findValue("uuid").asText();
                            String content = partNode
                                .findValue("content").asText();
                            Part part = new Part(uuid, content);
                            parts.add(part);
                        }
                    }
                    return parts;
                }
            });
    }

}
