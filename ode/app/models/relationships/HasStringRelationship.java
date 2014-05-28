package models.relationships;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.F.Function;
import play.libs.F.Promise;

import constants.RelationshipType;
import models.nodes.CombinationGroup;
import models.nodes.OutputString;
import managers.relationships.HasStringRelationshipManager;


public class HasStringRelationship extends TypedRelationship {

    public HasStringRelationship(CombinationGroup startNode,
                                 OutputString endNode) {
        super(RelationshipType.HAS, startNode, endNode);
    }

    public static Promise<List<OutputString>> getEndNodes(
        final CombinationGroup startNode) {
        Promise<List<JsonNode>> endNodes = HasStringRelationshipManager
            .getEndNodes(startNode);
        return endNodes.map(
            new Function<List<JsonNode>, List<OutputString>>() {
                public List<OutputString> apply(List<JsonNode> stringNodes) {
                    List<OutputString> strings =
                        new ArrayList<OutputString>();
                    for (JsonNode stringNode: stringNodes) {
                        if (!stringNode.has("position")) {
                            String uuid = stringNode
                                .findValue("uuid").asText();
                            String content = stringNode
                                .findValue("content").asText();
                            OutputString string = OutputString
                                .of(UUID.fromString(uuid), content);
                            strings.add(string);
                        }
                    }
                    return strings;
                }
            });
    }

}
