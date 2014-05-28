package models.relationships;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.F.Function;
import play.libs.F.Promise;

import constants.RelationshipType;
import models.nodes.CombinationGroup;
import models.nodes.Slot;
import managers.relationships.HasSlotRelationshipManager;


public class HasSlotRelationship extends TypedRelationship {

    public HasSlotRelationship(CombinationGroup startNode,
                               Slot endNode) {
        super(RelationshipType.HAS, startNode, endNode);
    }

    public static Promise<List<Slot>> getEndNodes(
        final CombinationGroup startNode) {
        Promise<List<JsonNode>> endNodes = HasSlotRelationshipManager
            .getEndNodes(startNode);
        return endNodes.map(
            new Function<List<JsonNode>, List<Slot>>() {
                public List<Slot> apply(List<JsonNode> slotNodes) {
                    List<Slot> slots = new ArrayList<Slot>();
                    for (JsonNode slotNode: slotNodes) {
                        if (slotNode.has("position")) {
                            String uuid = slotNode.findValue("uuid").asText();
                            int position =
                                slotNode.findValue("position").asInt();
                            Slot slot =
                                Slot.of(UUID.fromString(uuid), position);
                            slots.add(slot);
                        }
                    }
                    return slots;
                }
            });
    }

}
