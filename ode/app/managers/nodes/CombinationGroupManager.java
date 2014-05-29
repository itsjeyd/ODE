package managers.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import models.nodes.CombinationGroup;
import models.nodes.OutputString;
import models.nodes.Slot;
import models.relationships.Has;
import play.libs.F.Callback;
import play.libs.F.Function;
import play.libs.F.Promise;


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

}
