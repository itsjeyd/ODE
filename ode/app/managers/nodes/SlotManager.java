package managers.nodes;

import com.fasterxml.jackson.databind.JsonNode;
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
        JsonNode slot, JsonNode part, String location) {
        Slot s = new Slot(slot.get("uuid").asText());
        Part p = Part.of(UUID.fromString(part.get("uuid").asText()));
        return Has.relationships.delete(s, p, location);
    }

}
