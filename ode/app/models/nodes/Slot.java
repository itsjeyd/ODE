package models.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.F.Function;
import play.libs.F.Promise;

import constants.NodeType;
import managers.nodes.SlotManager;
import models.relationships.HasPartRelationship;
import models.relationships.HasSlotRelationship;


public class Slot extends LabeledNodeWithProperties {

    private Slot() {
        super(NodeType.SLOT);
    }

    private Slot(UUID uuid) {
        this();
        this.jsonProperties.put("uuid", uuid.toString());
    }

    private Slot(UUID uuid, int position) {
        this(uuid);
        this.jsonProperties.put("position", position);
    }

    public static Slot of(UUID uuid) {
        return new Slot(uuid);
    }

    public static Slot of(UUID uuid, int position) {
        return new Slot(uuid, position);
    }

    private Promise<List<Part>> getParts() {
        return HasPartRelationship.getEndNodes(this);
    }

    protected Promise<JsonNode> toJSON() {
        Promise<List<JsonNode>> partsList = this.getParts().map(
            new Function<List<Part>, List<JsonNode>>() {
                public List<JsonNode> apply(List<Part> parts) {
                    List<JsonNode> partList = new ArrayList<JsonNode>();
                    for (Part part: parts) {
                        JsonNode partJSON = part.toJSON();
                        partList.add(partJSON);
                    }
                    return partList;
                }
            });
        final ObjectNode json = this.jsonProperties.deepCopy();
        return partsList.map(
            new Function<List<JsonNode>, JsonNode>() {
                public JsonNode apply(List<JsonNode> partsList) {
                    ArrayNode parts = JsonNodeFactory.instance.arrayNode();
                    parts.addAll(partsList);
                    json.put("parts", parts);
                    return json;
                }
            });
    }

    public Promise<Boolean> create() {
        return SlotManager.create(this);
    }

    public Promise<Boolean> connectTo(final CombinationGroup group) {
        Promise<Boolean> created = this.create();
        return created.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean created) {
                    if (created) {
                        return new HasSlotRelationship(group, Slot.this)
                            .create();
                    }
                    return Promise.pure(false);
                }
            });
    }

    public Promise<Boolean> removeFrom(CombinationGroup group) {
        Promise<Boolean> disconnected = HasSlotRelationship
            .delete(group, this);
        return disconnected.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean disconnected) {
                    if (disconnected) {
                        return Slot.this.delete();
                    }
                    return Promise.pure(false);
                }
            });
    }

    public Promise<Boolean> addPart(Part part) {
        return part.connectTo(this);
    }

    public Promise<Boolean> removePart(Part part) {
        return part.removeFrom(this);
    }

    private Promise<Boolean> empty() {
        Promise<List<Part>> parts = this.getParts();
        Promise<List<Boolean>> removed = parts.flatMap(
            new Function<List<Part>, Promise<List<Boolean>>>() {
                public Promise<List<Boolean>> apply(List<Part> parts) {
                    List<Promise<? extends Boolean>> removed =
                        new ArrayList<Promise<? extends Boolean>>();
                    for (Part part: parts) {
                        removed.add(Slot.this.removePart(part));
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
                        return SlotManager.delete(Slot.this);
                    }
                    return Promise.pure(false);
                }
            });
    }

}
