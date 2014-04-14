package models.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.F.Tuple;

import constants.NodeType;
import managers.nodes.CombinationGroupManager;
import models.relationships.GroupRelationship;
import models.relationships.HasSlotRelationship;
import models.relationships.HasStringRelationship;


public class CombinationGroup extends LabeledNodeWithProperties {

    private CombinationGroup(UUID uuid) {
        super(NodeType.COMBINATION_GROUP);
        this.jsonProperties.put("uuid", uuid.toString());
    }

    public CombinationGroup(UUID uuid, int position) {
        super(NodeType.COMBINATION_GROUP);
        this.jsonProperties.put("uuid", uuid.toString());
        this.jsonProperties.put("position", position);
    }

    public static CombinationGroup of(String groupID) {
        return new CombinationGroup(UUID.fromString(groupID));
    }

    public static CombinationGroup of(String groupID, int position) {
        return new CombinationGroup(UUID.fromString(groupID), position);
    }

    private Promise<List<OutputString>> getStrings() {
        return HasStringRelationship.getEndNodes(this);
    }

    private Promise<List<Slot>> getSlots() {
        return HasSlotRelationship.getEndNodes(this);
    }

    protected Promise<JsonNode> toJSON() {
        Promise<List<JsonNode>> strings = this.getStrings().map(
            new Function<List<OutputString>, List<JsonNode>>() {
                public List<JsonNode> apply(
                    List<OutputString> outputStrings) {
                    List<JsonNode> stringList = new ArrayList<JsonNode>();
                    for (OutputString string: outputStrings) {
                        JsonNode stringJSON = string.toJSON();
                        stringList.add(stringJSON);
                    }
                    return stringList;
                }
            });
        Promise<List<JsonNode>> slots = this.getSlots().flatMap(
            new Function<List<Slot>, Promise<List<JsonNode>>>() {
                public Promise<List<JsonNode>> apply(List<Slot> slots) {
                    List<Promise<? extends JsonNode>> slotList =
                        new ArrayList<Promise<? extends JsonNode>>();
                    for (Slot slot: slots) {
                        Promise<JsonNode> slotJSON = slot.toJSON();
                        slotList.add(slotJSON);
                    }
                    return Promise.sequence(slotList);
                }
            });
        final ObjectNode json = this.jsonProperties.deepCopy();
        return strings.zip(slots).map(
            new Function<Tuple<List<JsonNode>, List<JsonNode>>, JsonNode>() {
                public JsonNode apply(
                    Tuple<List<JsonNode>, List<JsonNode>> components) {
                    ArrayNode strings = JsonNodeFactory.instance.arrayNode();
                    strings.addAll(components._1);
                    ArrayNode slots = JsonNodeFactory.instance.arrayNode();
                    slots.addAll(components._2);
                    json.put("outputStrings", strings);
                    ObjectNode partsTable = Json.newObject();
                    partsTable.put("slots", slots);
                    json.put("partsTable", partsTable);
                    return json;
                }
            });
    }

    public Promise<Boolean> create() {
        return CombinationGroupManager.create(this);
    }

    public Promise<Boolean> connectTo(RHS embeddingRHS) {
        return new GroupRelationship(embeddingRHS, this).create();
    }

    public Promise<Boolean> removeFrom(RHS embeddingRHS) {
        // - Delete ...
        Promise<Boolean> disconnected = GroupRelationship
            .delete(embeddingRHS, this);
        return disconnected.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean disconnected) {
                    if (disconnected) {
                        return CombinationGroup.this.delete();
                    }
                    return Promise.pure(false);
                }
            });
    }

    public Promise<Boolean> addString(OutputString string) {
        return string.connectTo(this);
    }

    public Promise<Boolean> removeString(OutputString string) {
        return string.removeFrom(this);
    }

    public Promise<Boolean> addSlot(Slot slot) {
        return slot.connectTo(this);
    }

    public Promise<Boolean> removeSlot(Slot slot) {
        return slot.removeFrom(this);
    }

    public Promise<Boolean> addRef(String slotID, String ruleName) {
        return null;
    }

    private Promise<Boolean> empty() {
        Promise<Boolean> stringsRemoved = this.removeStrings();
        Promise<Boolean> slotsRemoved = stringsRemoved.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean stringsRemoved) {
                    if (stringsRemoved) {
                        return CombinationGroup.this.removeSlots();
                    }
                    return Promise.pure(false);
                }
            });
        return slotsRemoved;
    }

    private Promise<Boolean> removeStrings() {
        Promise<List<OutputString>> strings = this.getStrings();
        Promise<List<Boolean>> removed = strings.flatMap(
            new Function<List<OutputString>, Promise<List<Boolean>>>() {
                public Promise<List<Boolean>> apply(
                    List<OutputString> strings) {
                    List<Promise<? extends Boolean>> removed =
                        new ArrayList<Promise<? extends Boolean>>();
                    for (OutputString string: strings) {
                        removed.add(
                            CombinationGroup.this.removeString(string));
                    }
                    return Promise.sequence(removed);
                }
            });
        return removed.map(
            new Function<List<Boolean>, Boolean>() {
                public Boolean apply(List<Boolean> removed) {
                    for (Boolean r: removed) {
                        if (!r) {
                            return false;
                        }
                    }
                    return true;
                }
            });
    }

    private Promise<Boolean> removeSlots() {
        Promise<List<Slot>> slots = this.getSlots();
        Promise<List<Boolean>> removed = slots.flatMap(
            new Function<List<Slot>, Promise<List<Boolean>>>() {
                public Promise<List<Boolean>> apply(List<Slot> slots) {
                    List<Promise<? extends Boolean>> removed =
                        new ArrayList<Promise<? extends Boolean>>();
                    for (Slot slot: slots) {
                        removed.add(
                            CombinationGroup.this.removeSlot(slot));
                    }
                    return Promise.sequence(removed);
                }
            });
        return removed.map(
            new Function<List<Boolean>, Boolean>() {
                public Boolean apply(List<Boolean> removed) {
                    for (Boolean r: removed) {
                        if (!r) {
                            return false;
                        }
                    }
                    return true;
                }
            });
    }

    public Promise<Boolean> delete() {
        Promise<Boolean> emptied = this.empty();
        return emptied.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean emptied) {
                    if (emptied) {
                        return CombinationGroupManager
                            .delete(CombinationGroup.this);
                    }
                    return Promise.pure(false);
                }
            });
    }

}
