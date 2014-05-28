package models.nodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import models.relationships.HasSlotRelationship;
import models.relationships.HasStringRelationship;


public class CombinationGroup extends LabeledNodeWithProperties {

    public static final CombinationGroupManager nodes =
        new CombinationGroupManager();

    public CombinationGroup() {
        super(NodeType.COMBINATION_GROUP);
    }

    public CombinationGroup(String uuid) {
        this();
        this.jsonProperties.put("uuid", uuid.toString());
    }

    public CombinationGroup(String uuid, int position) {
        this(uuid);
        this.jsonProperties.put("position", position);
    }

    private Promise<List<OutputString>> getStrings() {
        return HasStringRelationship.getEndNodes(this);
    }

    private Promise<List<Slot>> getSlots() {
        return HasSlotRelationship.getEndNodes(this);
    }

    private Promise<List<List<String>>> getStringsBySlots() {
        Promise<List<Slot>> slots = this.getSlots();
        Promise<List<List<Part>>> partsBySlots = slots.flatMap(
            new Function<List<Slot>, Promise<List<List<Part>>>>() {
                public Promise<List<List<Part>>> apply(List<Slot> slots) {
                    Collections.sort(
                        slots, new Comparator<Slot>() {
                            public int compare(Slot s1, Slot s2) {
                                Integer pos1 = s1.jsonProperties
                                    .get("position").asInt();
                                Integer pos2 = s2.jsonProperties
                                    .get("position").asInt();
                                return pos1.compareTo(pos2);
                            }
                        });
                    List<Promise<? extends List<Part>>> partsBySlots =
                        new ArrayList<Promise<? extends List<Part>>>();
                    for (Slot slot: slots) {
                        partsBySlots.add(slot.getParts());
                    }
                    return Promise.sequence(partsBySlots);
                }
            });
        Promise<List<List<String>>> stringsBySlots = partsBySlots.map(
            new Function<List<List<Part>>, List<List<String>>>() {
                public List<List<String>> apply(
                    List<List<Part>> partsBySlots) {
                    List<List<String>> stringsBySlots =
                        new ArrayList<List<String>>();
                    for (List<Part> partsBySlot: partsBySlots) {
                        List<String> stringsBySlot = new ArrayList<String>();
                        for (Part part: partsBySlot) {
                            stringsBySlot.add(part.content);
                        }
                        stringsBySlots.add(stringsBySlot);
                    }
                    return stringsBySlots;
                }
            });
        return stringsBySlots;
    }

    private List<String> cartesianProduct(List<List<String>> stringsBySlots) {
        if (stringsBySlots.isEmpty()) {
            return new ArrayList<String>();
        } else if (stringsBySlots.size() == 1) {
            return stringsBySlots.get(0);
        } else if (stringsBySlots.size() == 2) {
            return this
                .combineSlots(stringsBySlots.get(0), stringsBySlots.get(1));
        } else {
            List<String> intermediateResult = this
                .combineSlots(stringsBySlots.get(0), stringsBySlots.get(1));
            List<List<String>> remainingSlots =
                stringsBySlots.subList(2, stringsBySlots.size());
            remainingSlots.add(0, intermediateResult);
            return this.cartesianProduct(remainingSlots);
        }
    }

    private List<String> combineSlots(
        List<String> slot1, List<String> slot2) {
        if (slot1.isEmpty() && slot2.isEmpty()) {
            return new ArrayList<String>();
        }
        return this.combineSlots(slot1, slot2, new ArrayList<String>());
    }

    private List<String> combineSlots(
        List<String> slot1, List<String> slot2, List<String> result) {
        if (slot1.isEmpty()) {
            return result;
        }
        List<String> intermediateResult = this
            .combineStrings(slot1.get(0), slot2);
        result.addAll(intermediateResult);
        return this
            .combineSlots(slot1.subList(1, slot1.size()), slot2, result);
    }

    private List<String> combineStrings(String string, List<String> slot) {
        if (slot.isEmpty()) {
            List<String> result = new ArrayList<String>();
            result.add(string);
            return result;
        }
        return this.combineStrings(string, slot, new ArrayList<String>());
    }

    private List<String> combineStrings(
        String string, List<String> slot, List<String> result) {
        if (slot.isEmpty()) {
            return result;
        }
        String combinedString = string + " " + slot.get(0);
        result.add(combinedString);
        return this
            .combineStrings(string, slot.subList(1, slot.size()), result);
    }

    protected Promise<List<String>> findStrings(
        final List<String> searchStrings) {
        // 1. Get parts by slots
        Promise<List<List<String>>> stringsBySlots = this.getStringsBySlots();
        // 2. Combine
        Promise<List<String>> fullStrings = stringsBySlots.map(
            new Function<List<List<String>>, List<String>>() {
                public List<String> apply(List<List<String>> stringsBySlots) {
                    return CombinationGroup.this
                        .cartesianProduct(stringsBySlots);
                }
            });
        // 3. Check for search strings
        Promise<List<String>> stringsNotFound = fullStrings.map(
            new Function<List<String>, List<String>>() {
                public List<String> apply(List<String> fullStrings) {
                    List<String> stringsNotFound = new ArrayList<String>();
                    for (String searchString: searchStrings) {
                        boolean found = false;
                        for (String fullString: fullStrings) {
                            if (fullString.toLowerCase()
                                .contains(searchString.toLowerCase())) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            stringsNotFound.add(searchString);
                        }
                    }
                    return stringsNotFound;
                }
            });
        return stringsNotFound;
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

}
