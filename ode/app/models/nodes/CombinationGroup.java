package models.nodes;

import constants.NodeType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import managers.nodes.CombinationGroupManager;
import models.relationships.HasSlotRelationship;
import play.libs.F.Function;
import play.libs.F.Promise;


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

}
