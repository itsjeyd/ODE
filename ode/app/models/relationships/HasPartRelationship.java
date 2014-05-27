package models.relationships;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;

import play.libs.F.Function;
import play.libs.F.Promise;

import constants.RelationshipType;
import models.nodes.Part;
import models.nodes.Slot;
import managers.relationships.HasPartRelationshipManager;


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
                            Part part =
                                Part.of(UUID.fromString(uuid), content);
                            parts.add(part);
                        }
                    }
                    return parts;
                }
            });
    }

    public static Promise<Boolean> delete(Slot startNode, Part endNode) {
        return HasPartRelationshipManager.delete(startNode, endNode);
    }

}
