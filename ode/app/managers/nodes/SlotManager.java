package managers.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import constants.NodeType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import models.nodes.Part;
import models.nodes.Rule;
import models.nodes.Slot;
import models.relationships.Has;
import play.libs.F.Callback;
import play.libs.F.Function;
import play.libs.F.Promise;


public class SlotManager extends ContentCollectionNodeManager {

    public SlotManager() {
        this.label = NodeType.SLOT.toString();
    }

    // DELETE

    protected Promise<Boolean> empty(JsonNode properties, String location) {
        Slot slot = new Slot(properties.get("uuid").asText());
        return super.empty(slot, location);
    }

    // Connections to other nodes

    @Override
    protected Promise<Boolean> connect(
        JsonNode slot, JsonNode partOrRule, String location) {
        if (partOrRule.has("name")) {
            Rule rule = new Rule(partOrRule.get("name").asText());
            return connect(slot, rule, location);
        }
        Part part = new Part(
            partOrRule.get("uuid").asText(),
            partOrRule.get("content").asText());
        return connect(slot, part, location);
    }

    private Promise<Boolean> connect(
        final JsonNode slot, final Rule rule, final String location) {
        Promise<Boolean> exists = Rule.nodes.exists(rule.getProperties());
        Promise<Boolean> connected = exists.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean exists) {
                    if (exists) {
                        final Slot s = new Slot(slot.get("uuid").asText());
                        Promise<Boolean> connected = Has.relationships
                            .exists(s, rule);
                        return connected.flatMap(
                            new Function<Boolean, Promise<Boolean>>() {
                                public Promise<Boolean> apply(
                                    Boolean connected) {
                                    if (connected) {
                                        return Promise.pure(false);
                                    }
                                    return Has.relationships
                                        .create(s, rule, location);
                                }
                            });
                    }
                    return Promise.pure(false);
                }
            });
        return connected;
    }

    private Promise<Boolean> connect(
        final JsonNode slot, final Part part, final String location) {
        Promise<Boolean> exists = Part.nodes
            .create(part.getProperties(), location);
        Promise<Boolean> connected = exists.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean exists) {
                    if (exists) {
                        final Slot s = new Slot(slot.get("uuid").asText());
                        Promise<Boolean> connected = Has.relationships
                            .exists(s, part);
                        return connected.flatMap(
                            new Function<Boolean, Promise<Boolean>>() {
                                public Promise<Boolean> apply(
                                    Boolean connected) {
                                    if (connected) {
                                        return Promise.pure(false);
                                    }
                                    return Has.relationships
                                        .create(s, part, location);
                                }
                            });
                    }
                    return Promise.pure(false);
                }
            });
        return connected;
    }

    @Override
    protected Promise<Boolean> disconnect(
        JsonNode slot, JsonNode partOrRule, String location) {
        if (partOrRule.has("name")) {
            Rule rule = new Rule(partOrRule.get("name").asText());
            return disconnect(slot, rule, location);
        }
        String uuid = partOrRule.get("uuid").asText();
        Part part = new Part(UUID.fromString(uuid));
        return disconnect(slot, part, location);
    }

    private Promise<Boolean> disconnect(
        JsonNode slot, Rule rule, String location) {
        Slot s = new Slot(slot.get("uuid").asText());
        return Has.relationships.delete(s, rule, location);
    }

    private Promise<Boolean> disconnect(
        JsonNode slot, final Part part, String location) {
        Slot s = new Slot(slot.get("uuid").asText());
        Promise<Boolean> disconnected = Has.relationships
            .delete(s, part, location);
        disconnected.onRedeem(
            new Callback<Boolean>() {
                public void invoke (Boolean disconnected) {
                    if (disconnected) {
                        Part.nodes.delete(part.getProperties());
                    }
                }
            });
        return disconnected;
    }

    // Custom functionality

    protected Promise<JsonNode> toJSON(final JsonNode properties) {
        Slot slot = new Slot(properties.get("uuid").asText());
        Promise<List<JsonNode>> partsAndRefs = Has.relationships
            .endNodes(slot);
        Promise<JsonNode> json = partsAndRefs.map(
            new Function<List<JsonNode>, JsonNode>() {
                public JsonNode apply(List<JsonNode> partsAndRefs) {
                    List<JsonNode> partNodes = new ArrayList<JsonNode>();
                    List<JsonNode> refNodes = new ArrayList<JsonNode>();
                    for (JsonNode partOrRef: partsAndRefs) {
                        if (partOrRef.has("name")) {
                            String name = partOrRef.get("name").asText();
                            JsonNode ref = new TextNode(name);
                            refNodes.add(ref);
                        } else {
                            partNodes.add(partOrRef);
                        }
                    }
                    ArrayNode parts = JsonNodeFactory.instance.arrayNode();
                    parts.addAll(partNodes);
                    ((ObjectNode) properties).put("parts", parts);
                    ArrayNode refs = JsonNodeFactory.instance.arrayNode();
                    refs.addAll(refNodes);
                    ((ObjectNode) properties).put("refs", refs);
                    return properties;
                }
            });
        return json;
    }

    protected Promise<List<JsonNode>> parts(JsonNode properties) {
        Slot slot = new Slot(properties.get("uuid").asText());
        return Has.relationships.endNodesByLabel(slot, "Part");
    }

}
