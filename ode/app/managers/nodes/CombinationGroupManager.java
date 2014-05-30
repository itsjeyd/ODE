package managers.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import models.nodes.CombinationGroup;
import models.nodes.OutputString;
import models.nodes.Slot;
import models.relationships.Has;
import play.libs.F.Callback;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.Json;


public class CombinationGroupManager extends ContentCollectionNodeManager {

    public CombinationGroupManager() {
        this.label = "CombinationGroup";
    }

    // DELETE

    protected Promise<Boolean> empty(JsonNode properties, String location) {
        CombinationGroup group =
            new CombinationGroup(properties.get("uuid").asText());
        return super.empty(group, location);
    }

    @Override
    protected Promise<Boolean> disconnect(
        final JsonNode properties, List<JsonNode> stringsAndSlots,
        final String location) {
        Promise<Boolean> removed = Promise.pure(true);
        for (final JsonNode stringOrSlot : stringsAndSlots) {
            removed = removed.flatMap(
                new Function<Boolean, Promise<Boolean>>() {
                    public Promise<Boolean> apply(Boolean removed) {
                        if (removed) {
                            String uuid = stringOrSlot.get("uuid").asText();
                            if (stringOrSlot.has("position")) {
                                Slot slot = new Slot(uuid);
                                return disconnect(properties, slot, location);
                            }
                            OutputString string = new OutputString(uuid);
                            return disconnect(properties, string, location);
                        }
                        return Promise.pure(false);
                    }
                });
        }
        return removed;
    }

    // Connections to other nodes

    protected Promise<Boolean> connect(
        JsonNode group, JsonNode stringOrSlot, String location) {
        if (stringOrSlot.has("position")) {
            Slot slot = new Slot(
                stringOrSlot.get("uuid").asText(),
                stringOrSlot.get("position").asInt());
            return connect(group, slot, location);
        }
        OutputString string = new OutputString(
            stringOrSlot.get("uuid").asText(),
            stringOrSlot.get("content").asText());
        return connect(group, string, location);
    }

    private Promise<Boolean> connect(
        final JsonNode group, final Slot slot, final String location) {
        Promise<Boolean> created = Slot.nodes
            .create(slot.getProperties(), location);
        Promise<Boolean> connected = created.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean created) {
                    if (created) {
                        CombinationGroup g =
                            new CombinationGroup(group.get("uuid").asText());
                        return Has.relationships.create(g, slot, location);
                    }
                    return Promise.pure(false);
                }
            });
        return connected;
    }

    private Promise<Boolean> connect(
        final JsonNode group, final OutputString string,
        final String location) {
        Promise<Boolean> exists = OutputString.nodes
            .create(string.getProperties(), location);
        Promise<Boolean> connected = exists.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean exists) {
                    if (exists) {
                        final CombinationGroup g =
                            new CombinationGroup(group.get("uuid").asText());
                        Promise<Boolean> connected = Has.relationships
                            .exists(g, string);
                        return connected.flatMap(
                            new Function<Boolean, Promise<Boolean>>() {
                                public Promise<Boolean> apply(
                                    Boolean connected) {
                                    if (connected) {
                                        return Promise.pure(false);
                                    }
                                    return Has.relationships
                                        .create(g, string, location);
                                }
                            });

                    }
                    return Promise.pure(false);
                }
            });
        return connected;
    }

    protected Promise<Boolean> disconnect(
        JsonNode group, JsonNode stringOrSlot, String location) {
        String nodeType = "";
        if (stringOrSlot.has("nodeType")) {
            nodeType = stringOrSlot.get("nodeType").asText();
        }
        if (nodeType.equals("slot")) {
            Slot slot = new Slot(stringOrSlot.get("uuid").asText());
            return disconnect(group, slot, location);
        }
        OutputString string =
            new OutputString(stringOrSlot.get("uuid").asText());
        return disconnect(group, string, location);
    }

    private Promise<Boolean> disconnect(
        JsonNode group, final Slot slot, final String location) {
        CombinationGroup g = new CombinationGroup(group.get("uuid").asText());
        // 1. Disconnect group from slot
        Promise<Boolean> removed = Has.relationships
            .delete(g, slot, location);
        // 2. Delete slot
        removed = removed.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean removed) {
                    if (removed) {
                        return Slot.nodes
                            .delete(slot.getProperties(), location);
                    }
                    return Promise.pure(false);
                }
            });
        return removed;
    }

    private Promise<Boolean> disconnect(
        JsonNode group, final OutputString string, String location) {
        CombinationGroup g = new CombinationGroup(group.get("uuid").asText());
        Promise<Boolean> disconnected = Has.relationships
            .delete(g, string, location);
        disconnected.onRedeem(
            new Callback<Boolean>() {
                public void invoke (Boolean disconnected) {
                    if (disconnected) {
                        OutputString.nodes.delete(string.getProperties());
                    }
                }
            });
        return disconnected;
    }

    // Custom functionality

    protected Promise<JsonNode> toJSON(final JsonNode properties) {
        CombinationGroup group =
            new CombinationGroup(properties.get("uuid").asText());
        Promise<List<JsonNode>> stringsAndSlots = Has.relationships
            .endNodes(group);
        Promise<JsonNode> json = stringsAndSlots.flatMap(
            new Function<List<JsonNode>, Promise<JsonNode>>() {
                public Promise<JsonNode> apply(
                    List<JsonNode> stringsAndSlots) {
                    final List<JsonNode> stringNodes =
                        new ArrayList<JsonNode>();
                    List<Promise<? extends JsonNode>> slots =
                        new ArrayList<Promise<? extends JsonNode>>();
                    for (JsonNode stringOrSlot: stringsAndSlots) {
                        if (stringOrSlot.has("position")) {
                            Promise<JsonNode> slot = Slot.nodes
                                .toJSON(stringOrSlot);
                            slots.add(slot);
                        } else {
                            JsonNode string = OutputString.nodes
                                .toJSON(stringOrSlot);
                            stringNodes.add(string);
                        }
                    }
                    Promise<List<JsonNode>> slotNodes =
                        Promise.sequence(slots);
                    Promise<JsonNode> json = slotNodes.map(
                        new Function<List<JsonNode>, JsonNode>() {
                            public JsonNode apply(List<JsonNode> slotNodes) {
                                ArrayNode strings =
                                    JsonNodeFactory.instance.arrayNode();
                                strings.addAll(stringNodes);
                                ((ObjectNode) properties)
                                    .put("outputStrings", strings);
                                ArrayNode slots =
                                    JsonNodeFactory.instance.arrayNode();
                                slots.addAll(slotNodes);
                                ObjectNode partsTable = Json.newObject();
                                partsTable.put("slots", slots);
                                ((ObjectNode) properties)
                                    .put("partsTable", partsTable);
                                return properties;
                            }
                        });
                    return json;
                }
            });
        return json;
    }

    protected Promise<List<String>> find(
        JsonNode properties, final List<String> strings) {
        // 1. Get parts by slots
        Promise<List<List<String>>> stringsBySlots =
            getStringsBySlots(properties);
        // 2. Combine
        Promise<List<String>> fullStrings = stringsBySlots.map(
            new Function<List<List<String>>, List<String>>() {
                public List<String> apply(List<List<String>> stringsBySlots) {
                    return cartesianProduct(stringsBySlots);
                }
            });
        // 3. Check for search strings
        Promise<List<String>> stringsNotFound = fullStrings.map(
            new Function<List<String>, List<String>>() {
                public List<String> apply(List<String> fullStrings) {
                    List<String> stringsNotFound = new ArrayList<String>();
                    for (String string: strings) {
                        boolean found = false;
                        for (String fullString: fullStrings) {
                            if (fullString.toLowerCase()
                                .contains(string.toLowerCase())) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            stringsNotFound.add(string);
                        }
                    }
                    return stringsNotFound;
                }
            });
        return stringsNotFound;
    }

    private Promise<List<List<String>>> getStringsBySlots(
        JsonNode properties) {
        Promise<List<JsonNode>> slots = slots(properties);
        Promise<List<List<JsonNode>>> partsBySlots = slots.flatMap(
            new Function<List<JsonNode>, Promise<List<List<JsonNode>>>>() {
                public Promise<List<List<JsonNode>>> apply(List<JsonNode> slots) {
                    Collections.sort(
                        slots, new Comparator<JsonNode>() {
                            public int compare(JsonNode s1, JsonNode s2) {
                                Integer pos1 = s1.get("position").asInt();
                                Integer pos2 = s2.get("position").asInt();
                                return pos1.compareTo(pos2);
                            }
                        });
                    List<Promise<? extends List<JsonNode>>> partsBySlots =
                        new ArrayList<Promise<? extends List<JsonNode>>>();
                    for (JsonNode slot: slots) {
                        partsBySlots.add(Slot.nodes.parts(slot));
                    }
                    return Promise.sequence(partsBySlots);
                }
            });
        Promise<List<List<String>>> stringsBySlots = partsBySlots.map(
            new Function<List<List<JsonNode>>, List<List<String>>>() {
                public List<List<String>> apply(
                    List<List<JsonNode>> partsBySlots) {
                    List<List<String>> stringsBySlots =
                        new ArrayList<List<String>>();
                    for (List<JsonNode> partsBySlot: partsBySlots) {
                        List<String> stringsBySlot = new ArrayList<String>();
                        for (JsonNode part: partsBySlot) {
                            stringsBySlot.add(part.get("content").asText());
                        }
                        stringsBySlots.add(stringsBySlot);
                    }
                    return stringsBySlots;
                }
            });
        return stringsBySlots;
    }

    private Promise<List<JsonNode>> slots(JsonNode properties) {
        CombinationGroup group =
            new CombinationGroup(properties.get("uuid").asText());
        return Has.relationships.endNodesByLabel(group, "Slot");
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

}
