package models.relationships;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.F.Function;
import play.libs.F.Promise;

import constants.RelationshipType;
import managers.relationships.GroupRelationshipManager;
import models.nodes.CombinationGroup;
import models.nodes.RHS;


public class GroupRelationship extends TypedRelationship {

    public GroupRelationship(RHS startNode, CombinationGroup endNode) {
        super(RelationshipType.HAS, startNode, endNode);
    }

    public static Promise<List<CombinationGroup>> getEndNodes(
        final RHS startNode) {
        Promise<List<JsonNode>> endNodes = GroupRelationshipManager
            .getEndNodes(startNode);
        return endNodes.map(
            new Function<List<JsonNode>, List<CombinationGroup>>() {
                public List<CombinationGroup> apply(
                    List<JsonNode> groupNodes) {
                    List<CombinationGroup> groups =
                        new ArrayList<CombinationGroup>();
                    for (JsonNode groupNode: groupNodes) {
                        String uuid = groupNode.findValue("uuid").asText();
                        int position =
                            groupNode.findValue("position").asInt();
                        CombinationGroup group =
                            CombinationGroup.of(uuid, position);
                        groups.add(group);
                    }
                    return groups;
                }
            });
    }

}
