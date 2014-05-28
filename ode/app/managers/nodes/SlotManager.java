package managers.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.UUID;
import models.nodes.Part;
import models.nodes.Rule;
import models.nodes.Slot;
import models.relationships.Has;
import play.libs.F.Function;
import play.libs.F.Promise;


public class SlotManager extends LabeledNodeWithPropertiesManager {

    public SlotManager() {
        this.label = "Slot";
    }

    @Override
    protected Promise<Boolean> delete(
        final JsonNode properties, final String location) {
        // 1. Empty slot
        Promise<Boolean> emptied = empty(properties, location);
        // 2. Delete slot
        Promise<Boolean> deleted = emptied.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean emptied) {
                    if (emptied) {
                        return SlotManager.super.delete(properties, location);
                    }
                    return Promise.pure(false);
                }
            });
        return deleted;
    }

    private Promise<Boolean> empty(
        final JsonNode properties, final String location) {
        Slot slot = new Slot(properties.get("uuid").asText());
        Promise<List<JsonNode>> partsAndRefs = Has.relationships
            .endNodes(slot, location);
        Promise<Boolean> emptied = partsAndRefs.flatMap(
            new Function<List<JsonNode>, Promise<Boolean>>() {
                public Promise<Boolean> apply(List<JsonNode> partsAndRefs) {
                    return disconnect(properties, partsAndRefs, location);
                }
            });
        return emptied;
    }

    private Promise<Boolean> disconnect(
        final JsonNode properties, List<JsonNode> partsAndRefs,
        final String location) {
        Promise<Boolean> removed = Promise.pure(true);
        for (final JsonNode partOrRef: partsAndRefs) {
            removed = removed.flatMap(
                new Function<Boolean, Promise<Boolean>>() {
                    public Promise<Boolean> apply(Boolean removed) {
                        if (removed) {
                            return disconnect(properties, partOrRef, location);
                        }
                        return Promise.pure(false);
                    }
                });
        }
        return removed;
    }

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
        JsonNode slot, Rule rule, String location) {
        Slot s = new Slot(slot.get("uuid").asText());
        return Has.relationships.create(s, rule, location);
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

    protected Promise<Boolean> disconnect(
        JsonNode slot, JsonNode partOrRule, String location) {
        if (partOrRule.has("name")) {
            Rule rule = new Rule(partOrRule.get("name").asText());
            return disconnect(slot, rule, location);
        }
        Part part = Part.of(UUID.fromString(partOrRule.get("uuid").asText()));
        return disconnect(slot, part, location);
    }

    private Promise<Boolean> disconnect(
        JsonNode slot, Rule rule, String location) {
        Slot s = new Slot(slot.get("uuid").asText());
        return Has.relationships.delete(s, rule, location);
    }

    private Promise<Boolean> disconnect(
        JsonNode slot, Part part, String location) {
        Slot s = new Slot(slot.get("uuid").asText());
        return Has.relationships.delete(s, part, location);
    }

}
