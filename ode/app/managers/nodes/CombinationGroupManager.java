package managers.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import models.nodes.CombinationGroup;
import models.nodes.OutputString;
import models.nodes.Slot;
import models.relationships.Has;
import play.libs.F.Function;
import play.libs.F.Promise;


public class CombinationGroupManager extends
                                         LabeledNodeWithPropertiesManager {

    public CombinationGroupManager() {
        this.label = "CombinationGroup";
    }

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
        JsonNode group, Slot slot, String location) {
        CombinationGroup g = new CombinationGroup(group.get("uuid").asText());
        return Has.relationships.create(g, slot, location);
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
        JsonNode group, JsonNode string, String location) {
        CombinationGroup g = new CombinationGroup(group.get("uuid").asText());
        OutputString s = new OutputString(string.get("uuid").asText());
        return Has.relationships.delete(g, s, location);
    }

}
