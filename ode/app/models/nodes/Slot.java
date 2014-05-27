package models.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.F.Tuple;

import constants.NodeType;
import managers.nodes.SlotManager;
import models.relationships.HasPartRelationship;
import models.relationships.HasRefRelationship;
import models.relationships.HasSlotRelationship;


public class Slot extends LabeledNodeWithProperties {

    public static final SlotManager nodes = new SlotManager();

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

    public Slot(String uuid) {
        this(UUID.fromString(uuid));
    }

    public Slot(String uuid, int position) {
        this(UUID.fromString(uuid));
        this.jsonProperties.put("position", position);
    }

    public static Slot of(UUID uuid) {
        return new Slot(uuid);
    }

    public static Slot of(UUID uuid, int position) {
        return new Slot(uuid, position);
    }

    protected Promise<List<Part>> getParts() {
        return HasPartRelationship.getEndNodes(this);
    }

    private Promise<List<Rule>> getRefs() {
        return HasRefRelationship.getEndNodes(this);
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
        Promise<List<JsonNode>> refsList = this.getRefs().map(
            new Function<List<Rule>, List<JsonNode>>() {
                public List<JsonNode> apply(List<Rule> refs) {
                    List<JsonNode> refList = new ArrayList<JsonNode>();
                    for (Rule rule: refs) {
                        JsonNode ref = new TextNode(rule.name);
                        refList.add(ref);
                    }
                    return refList;
                }
            });
        final ObjectNode json = this.jsonProperties.deepCopy();
        return partsList.zip(refsList).map(
            new Function<Tuple<List<JsonNode>, List<JsonNode>>, JsonNode>() {
                public JsonNode apply(
                    Tuple<List<JsonNode>, List<JsonNode>> contents) {
                    ArrayNode parts = JsonNodeFactory.instance.arrayNode();
                    parts.addAll(contents._1);
                    json.put("parts", parts);
                    ArrayNode refs = JsonNodeFactory.instance.arrayNode();
                    refs.addAll(contents._2);
                    json.put("refs", refs);
                    return json;
                }
            });
    }

    public Promise<Boolean> create() {
        return null;
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

    public Promise<Boolean> addRef(Rule rule) {
        return rule.connectTo(this);
    }

    public Promise<Boolean> removeRef(Rule rule) {
        return rule.removeFrom(this);
    }

    private Promise<Boolean> empty() {
        Promise<List<Part>> parts = this.getParts();
        Promise<List<Boolean>> partsRemoved = parts.flatMap(
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
        Promise<List<Rule>> refs = this.getRefs();
        Promise<List<Boolean>> refsRemoved = refs.flatMap(
            new Function<List<Rule>, Promise<List<Boolean>>>() {
                public Promise<List<Boolean>> apply(List<Rule> refs) {
                    List<Promise<? extends Boolean>> removed =
                        new ArrayList<Promise<? extends Boolean>>();
                    for (Rule ref: refs) {
                        removed.add(Slot.this.removeRef(ref));
                    }
                    return Promise.sequence(removed);
                }
            });
        return partsRemoved.zip(refsRemoved).map(
            new Function<Tuple<List<Boolean>, List<Boolean>>, Boolean>() {
                public Boolean apply(
                    Tuple<List<Boolean>, List<Boolean>> removed) {
                    for (Boolean r: removed._1) {
                        if (!r) {
                            return false;
                        }
                    }
                    for (Boolean r: removed._2) {
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
